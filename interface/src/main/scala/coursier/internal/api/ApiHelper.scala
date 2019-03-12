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

  private def apiModule(m: Module): coursierapi.Module =
    coursierapi.Module.of(
      m.organization.value,
      m.name.value,
      m.attributes.asJava
    )

  def parseModule(s: String, scalaVersion: String): coursierapi.Module =
    coursier.util.Parse.module(s, scalaVersion) match {
      case Left(err) =>
        throw new IllegalArgumentException(err)
      case Right(m) =>
        apiModule(m)
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

  def dependency(dep: coursierapi.Dependency): Dependency =
    Dependency(
      Module(
        Organization(dep.getModule.getOrganization),
        ModuleName(dep.getModule.getName),
        dep.getModule.getAttributes.asScala.iterator.toMap
      ),
      dep.getVersion
    )

  def dependency(dep: Dependency): coursierapi.Dependency =
    coursierapi.Dependency
      .of(
        apiModule(dep.module),
        dep.version
      )
      .withExclusion(
        dep
          .exclusions
          .map {
            case (o, n) =>
              new ju.AbstractMap.SimpleImmutableEntry(o.value, n.value): ju.Map.Entry[String, String]
          }
          .asJava
      )

  def fetch(fetch: coursierapi.Fetch): Fetch[Task] = {

    val dependencies = fetch
      .getDependencies
      .asScala
      .map(dependency)

    val repositories = fetch
      .getRepositories
      .asScala
      .map {
        case ApiRepo(repo) => repo
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

    Fetch()
      .withDependencies(dependencies)
      .withRepositories(repositories)
      .withCache(cache)
      .withMainArtifacts(fetch.getMainArtifacts)
      .withClassifiers(classifiers)
      .withFetchCache(Option(fetch.getFetchCache))
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
