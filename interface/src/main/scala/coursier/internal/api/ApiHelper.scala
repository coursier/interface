package coursier.internal.api

import java.io.{File, OutputStreamWriter}
import java.{util => ju}
import java.util.concurrent.ExecutorService

import coursier._
import coursierapi.{Credentials, Logger, SimpleLogger}
import coursier.cache.loggers.RefreshLogger
import coursier.cache.{CacheDefaults, CacheLogger, FileCache}
import coursier.core.{Authentication, Configuration}
import coursier.error.{CoursierError, FetchError, ResolutionError}
import coursier.ivy.IvyRepository
import coursier.params.ResolutionParams
import coursier.util.Parse.ModuleRequirements
import coursier.util.Task

import scala.collection.JavaConverters._

object ApiHelper {

  private[this] final case class ApiRepo(repo: Repository) extends coursierapi.Repository

  def defaultRepositories(): Array[coursierapi.Repository] =
    Resolve.defaultRepositories
      .map { repo =>
        ApiRepo(repo)
      }
      .toArray

  def defaultPool(): ExecutorService =
    CacheDefaults.pool
  def defaultLocation(): File =
    CacheDefaults.location

  def progressBarLogger(writer: OutputStreamWriter): Logger =
    WrappedLogger.of(RefreshLogger.create(writer))
  def nopLogger(): Logger =
    WrappedLogger.of(CacheLogger.nop)

  def parseModule(s: String, scalaVersion: String): coursierapi.Module =
    coursier.util.Parse.module(s, scalaVersion) match {
      case Left(err) =>
        throw new IllegalArgumentException(err)
      case Right(m) =>
        module(m)
    }

  def parseDependency(s: String, scalaVersion: String): coursierapi.Dependency =
    coursier.util.Parse.moduleVersionConfig(
      s,
      ModuleRequirements(defaultConfiguration = Configuration.empty),
      transitive = true,
      scalaVersion
    ) match {
      case Left(err) =>
        throw new IllegalArgumentException(err)
      case Right((dep, params)) =>
        // TODO Handle other Dependency fields, and params
        dependency(dep)
    }

  private[this] def authenticationOpt(credentials: Credentials): Option[Authentication] =
    if (credentials == null)
      None
    else
      Some(Authentication(credentials.getUser, credentials.getPassword))

  private[this] def ivyRepository(ivy: coursierapi.IvyRepository): IvyRepository =
    IvyRepository.parse(
      ivy.getPattern,
      Option(ivy.getMetadataPattern).filter(_.nonEmpty),
      authentication = authenticationOpt(ivy.getCredentials)
    ) match {
      case Left(err) =>
        throw new Exception(s"Invalid Ivy repository $ivy: $err")
      case Right(repo) => repo
    }

  def validateIvyRepository(ivy: coursierapi.IvyRepository): Unit =
    ivyRepository(ivy) // throws if anything's wrong

  def module(mod: coursierapi.Module): Module =
    Module(
      Organization(mod.getOrganization),
      ModuleName(mod.getName),
      mod.getAttributes.asScala.iterator.toMap
    )

  def module(mod: Module): coursierapi.Module =
    coursierapi.Module.of(
      mod.organization.value,
      mod.name.value,
      mod.attributes.asJava
    )

  def dependency(dep: coursierapi.Dependency): Dependency = {

    val module0 = module(dep.getModule)
    val exclusions = dep
      .getExclusions
      .iterator()
      .asScala
      .map(e => (Organization(e.getKey), ModuleName(e.getValue)))
      .toSet
    val configuration = Configuration(dep.getConfiguration)
    val tpe = Type(dep.getType)
    val classifier = Classifier(dep.getClassifier)

    Dependency(
      module0, dep.getVersion,
      exclusions = exclusions,
      configuration = configuration,
      attributes = Attributes(tpe, classifier),
      transitive = dep.isTransitive
    )
  }

  def dependency(dep: Dependency): coursierapi.Dependency =
    coursierapi.Dependency
      .of(
        module(dep.module),
        dep.version
      )
      .withConfiguration(dep.configuration.value)
      .withType(dep.attributes.`type`.value)
      .withClassifier(dep.attributes.classifier.value)
      .withExclusion(
        dep
          .exclusions
          .map {
            case (o, n) =>
              new ju.AbstractMap.SimpleImmutableEntry(o.value, n.value): ju.Map.Entry[String, String]
          }
          .asJava
      )
      .withTransitive(dep.transitive)

  def repository(repo: coursierapi.Repository): Repository =
    repo match {
      case ApiRepo(repo0) => repo0
      case mvn: coursierapi.MavenRepository =>
        MavenRepository(
          mvn.getBase,
          authentication = authenticationOpt(mvn.getCredentials)
        )
      case ivy: coursierapi.IvyRepository =>
        ivyRepository(ivy)
      case other =>
        throw new Exception(s"Unrecognized repository: " + other)
    }

  def credentials(auth: Authentication): Credentials =
    coursierapi.Credentials.of(auth.user, auth.password)

  def repository(repo: Repository): coursierapi.Repository =
    repo match {
      case mvn: MavenRepository =>
        val credentialsOpt = mvn.authentication.map(credentials)
        coursierapi.MavenRepository.of(mvn.root)
          .withCredentials(credentialsOpt.orNull)
      case ivy: IvyRepository =>
        val credentialsOpt = ivy.authentication.map(credentials)
        val mdPatternOpt = ivy.metadataPatternOpt.map(_.string)
        coursierapi.IvyRepository.of(ivy.pattern.string)
          .withMetadataPattern(mdPatternOpt.orNull)
          .withCredentials(credentialsOpt.orNull)
      case other =>
        throw new Exception(s"Unrecognized repository: " + other)
    }

  def resolutionParams(params: ResolutionParams): coursierapi.ResolutionParams = {
    val default = ResolutionParams()
    var params0 = coursierapi.ResolutionParams.create()
    if (params.maxIterations != default.maxIterations)
      params0 = params0.withMaxIterations(params.maxIterations)
    params0
      .withForceVersions(params.forceVersion.map { case (m, v) => module(m) -> v }.asJava)
      .withForceProperties(params.forcedProperties.asJava)
  }

  def resolutionParams(params: coursierapi.ResolutionParams): ResolutionParams = {
    var params0 = ResolutionParams()
    if (params.getMaxIterations != null)
      params0 = params0.withMaxIterations(params.getMaxIterations)
    params0
      .withForceVersion(params.getForceVersions.asScala.iterator.toMap.map { case (m, v) => module(m) -> v })
      .withForcedProperties(params.getForcedProperties.asScala.iterator.toMap)
  }

  def fetch(fetch: coursierapi.Fetch): Fetch[Task] = {

    val dependencies = fetch
      .getDependencies
      .asScala
      .map(dependency)

    val repositories = fetch
      .getRepositories
      .asScala
      .map(repository)

    val loggerOpt = Option(fetch.getCache.getLogger).map[CacheLogger] {
      case s: SimpleLogger =>
        new CacheLogger {
          override def downloadingArtifact(url: String) =
            s.starting(url)
          override def downloadLength(url: String, totalLength: Long, alreadyDownloaded: Long, watching: Boolean) =
            s.length(url, totalLength, alreadyDownloaded, watching)
          override def downloadProgress(url: String, downloaded: Long) =
            s.progress(url, downloaded)
          override def downloadedArtifact(url: String, success: Boolean) =
            s.done(url, success)
        }
      case w: WrappedLogger =>
        w.getLogger
    }

    val cache = FileCache()
      .withPool(fetch.getCache.getPool)
      .withLocation(fetch.getCache.getLocation)
      .withLogger(loggerOpt.getOrElse(CacheLogger.nop))

    val classifiers = fetch
      .getClassifiers
      .asScala
      .iterator
      .toSet[String]
      .map(Classifier(_))

    val params = resolutionParams(fetch.getResolutionParams)

    var f = Fetch()
      .withDependencies(dependencies)
      .withRepositories(repositories)
      .withCache(cache)
      .withMainArtifacts(fetch.getMainArtifacts)
      .withClassifiers(classifiers)
      .withFetchCache(Option(fetch.getFetchCache))
      .withResolutionParams(params)
    if (fetch.getArtifactTypes != null)
      f = f.withArtifactTypes(fetch.getArtifactTypes.asScala.toSet[String].map(Type(_)))
    f
  }

  private def simpleResError(err: ResolutionError.Simple): coursierapi.error.SimpleResolutionError =
    err match {
      // TODO Handle specific implementations of Simple
      case s: ResolutionError.Simple =>
        coursierapi.error.SimpleResolutionError.of(s.getMessage)
    }

  def doFetch(apiFetch: coursierapi.Fetch): Array[File] = {

    val either = fetch(apiFetch).either()

    // TODO Pass exception causes if any

    either match {
      case Left(err) =>

        val ex = err match {
          case d: FetchError.DownloadingArtifacts =>
            coursierapi.error.DownloadingArtifactsError.of(
              d.errors.map { case (a, e) => a.url -> e.describe }.toMap.asJava
            )
          case f: FetchError =>
            coursierapi.error.FetchError.of(f.getMessage)

          case s: ResolutionError.Several =>
            coursierapi.error.MultipleResolutionError.of(
              simpleResError(s.head),
              s.tail.map(simpleResError): _*
            )
          case s: ResolutionError.Simple =>
            simpleResError(s)
          case r: ResolutionError =>
            coursierapi.error.ResolutionError.of(r.getMessage)

          case c: CoursierError =>
            coursierapi.error.CoursierError.of(c.getMessage)
        }

        throw ex

      case Right(res) => res.toArray
    }
  }

}
