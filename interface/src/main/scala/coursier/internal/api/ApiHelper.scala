package coursier.internal.api

import java.io.{File, OutputStreamWriter}
import java.time.LocalDateTime
import java.{util => ju}
import java.util.concurrent.ExecutorService
import coursier._
import coursierapi.{Credentials, Logger, SimpleLogger}
import coursier.cache.loggers.RefreshLogger
import coursier.cache.{ArchiveCache, CacheDefaults, CacheLogger, FileCache, UnArchiver}
import coursier.core.{Authentication, Configuration, Version}
import coursier.error.{CoursierError, FetchError, ResolutionError}
import coursier.ivy.IvyRepository
import coursier.jvm.{JavaHome, JvmCache}
import coursier.params.ResolutionParams
import coursier.util.{Artifact, Task}

import scala.collection.JavaConverters
import scala.collection.JavaConverters._

object ApiHelper {

  private[this] final case class ApiRepo(repo: Repository) extends coursierapi.Repository

  def defaultRepositories(): Array[coursierapi.Repository] =
    Resolve.defaultRepositories
      .map { repo =>
        ApiRepo(repo)
      }
      .toArray

  def ivy2Local(): coursierapi.IvyRepository = {
    val repo = coursierapi.IvyRepository.of(
      LocalRepositories.ivy2Local.pattern.string,
      LocalRepositories.ivy2Local.metadataPatternOpt.map(_.string).orNull
    )
    repo.withDropInfoAttributes(LocalRepositories.ivy2Local.dropInfoAttributes)
  }

  def central(): coursierapi.MavenRepository =
    coursierapi.MavenRepository.of(Repositories.central.root)

  def defaultPool(): ExecutorService =
    CacheDefaults.pool
  def defaultLocation(): File =
    CacheDefaults.location
  def defaultArchiveCacheLocation(): File =
    CacheDefaults.archiveCacheLocation

  def progressBarLogger(writer: OutputStreamWriter): Logger =
    WrappedLogger.of(RefreshLogger.create(writer))
  def nopLogger(): Logger =
    WrappedLogger.of(CacheLogger.nop)

  def parseModule(s: String, scalaVersion: String): coursierapi.Module =
    coursier.parse.ModuleParser.module(s, scalaVersion) match {
      case Left(err) =>
        throw new IllegalArgumentException(err)
      case Right(m) =>
        module(m)
    }

  def parseDependency(s: String, scalaVersion: String): coursierapi.Dependency =
    coursier.parse.DependencyParser.dependency(s, scalaVersion, Configuration.empty) match {
      case Left(err) =>
        throw new IllegalArgumentException(err)
      case Right(dep) =>
        // TODO Handle other Dependency fields
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
      authentication = authenticationOpt(ivy.getCredentials),
      dropInfoAttributes = ivy.getDropInfoAttributes
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

    Dependency(module0, dep.getVersion)
      .withExclusions(exclusions)
      .withConfiguration(configuration)
      .withAttributes(Attributes(tpe, classifier))
      .withTransitive(dep.isTransitive)
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
    coursierapi.Credentials.of(auth.user, auth.passwordOpt.getOrElse(""))

  def credentials(credentials: Credentials): Authentication =
    Authentication(credentials.getUser, credentials.getPassword)

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
      .withProfiles(params.profiles.asJava)
      .withExclusions(params.exclusions.map { case (o, n) => new ju.AbstractMap.SimpleEntry(o.value, n.value): ju.Map.Entry[String, String] }.asJava)
      .withUseSystemOsInfo(params.useSystemOsInfo)
      .withUseSystemJdkVersion(params.useSystemJdkVersion)
      .withScalaVersion(params.scalaVersionOpt.orNull)
  }

  def resolutionParams(params: coursierapi.ResolutionParams): ResolutionParams = {
    var params0 = ResolutionParams()
    if (params.getMaxIterations != null)
      params0 = params0.withMaxIterations(params.getMaxIterations)
    params0
      .withForceVersion(params.getForceVersions.asScala.iterator.toMap.map { case (m, v) => module(m) -> v })
      .withForcedProperties(params.getForcedProperties.asScala.iterator.toMap)
      .withProfiles(params.getProfiles.asScala.toSet)
      .withExclusions(params.getExclusions.asScala.map { e => (Organization(e.getKey), ModuleName(e.getValue)) }.toSet)
      .withUseSystemOsInfo(params.getUseSystemOsInfo)
      .withUseSystemJdkVersion(params.getUseSystemJdkVersion)
      .withScalaVersionOpt(Option(params.getScalaVersion))
  }

  def cache(cache: coursierapi.Cache): FileCache[Task] = {

    val loggerOpt = Option(cache.getLogger).map[CacheLogger] {
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

    FileCache()
      .withPool(cache.getPool)
      .withLocation(cache.getLocation)
      .withLogger(loggerOpt.getOrElse(CacheLogger.nop))
  }

  def cache(cache: FileCache[Task]): coursierapi.Cache = {

    val loggerOpt = cache.logger match {
      // case CacheLogger.nop => None
      case logger => Some(WrappedLogger.of(logger))
    }

    coursierapi.Cache.create()
      .withPool(cache.pool)
      .withLocation(cache.location)
      .withLogger(loggerOpt.orNull)
  }

  def fetch(fetch: coursierapi.Fetch): Fetch[Task] = {

    val dependencies = fetch
      .getDependencies
      .asScala
      .map(dependency)
      .toVector

    val repositories = fetch
      .getRepositories
      .asScala
      .map(repository)
      .toVector

    val cache0 = cache(fetch.getCache)

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
      .withCache(cache0)
      .withMainArtifacts(fetch.getMainArtifacts)
      .withClassifiers(classifiers)
      .withFetchCache(Option(fetch.getFetchCacheIKnowWhatImDoing))
      .withResolutionParams(params)
    if (fetch.getArtifactTypes != null)
      f = f.withArtifactTypes(fetch.getArtifactTypes.asScala.toSet[String].map(Type(_)))
    f
  }

  def fetch(fetch: Fetch[Task]): coursierapi.Fetch = {

    val dependencies = fetch
      .dependencies
      .map(dependency)

    val repositories = fetch
      .repositories
      .map(repository)

    val cache0 = cache(
      fetch.cache match {
        case f: FileCache[Task] => f
        case c => sys.error(s"Unsupported cache type: $c")
      }
    )

    val classifiers = JavaConverters.setAsJavaSet(
      fetch
        .classifiers
        .map(_.value)
    )

    val params = resolutionParams(fetch.resolutionParams)

    val artifactTypesOpt = fetch
      .artifactTypesOpt
      .map(s => JavaConverters.setAsJavaSet(s.map(_.value)))

    coursierapi.Fetch.create()
      .withDependencies(dependencies: _*)
      .withRepositories(repositories: _*)
      .withCache(cache0)
      .withMainArtifacts(fetch.mainArtifactsOpt.map(b => b: java.lang.Boolean).orNull)
      .withClassifiers(classifiers)
      .withFetchCacheIKnowWhatImDoing(fetch.fetchCacheOpt.orNull)
      .withResolutionParams(params)
      .withArtifactTypes(artifactTypesOpt.orNull)
  }

  private def simpleResError(err: ResolutionError.Simple): coursierapi.error.SimpleResolutionError =
    err match {
      // TODO Handle specific implementations of Simple
      case s: ResolutionError.Simple =>
        coursierapi.error.SimpleResolutionError.of(s.getMessage)
    }

  private def artifact(artifact: Artifact): coursierapi.Artifact = {
    val credentials0 = artifact.authentication.map(credentials).orNull
    coursierapi.Artifact.of(artifact.url, artifact.changing, artifact.optional, credentials0)
  }

  private def artifact(artifact: coursierapi.Artifact): Artifact =
    Artifact(artifact.getUrl)
      .withChanging(artifact.isChanging)
      .withOptional(artifact.isOptional)
      .withAuthentication(Option(artifact.getCredentials).map(credentials))

  def doFetch(apiFetch: coursierapi.Fetch): coursierapi.FetchResult = {

    val fetch0 = fetch(apiFetch)
    val either =
      if (apiFetch.getFetchCacheIKnowWhatImDoing == null)
        fetch0.eitherResult()
      else {
        val dummyArtifact = Artifact("", Map(), Map(), changing = false, optional = false, None)
        fetch0.either().map(files => Fetch.Result().withExtraArtifacts(files.map((dummyArtifact, _))))
      }

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

      case Right(result) =>
        val artifactFiles = new ju.ArrayList[ju.Map.Entry[coursierapi.Artifact, File]]
        for ((a, f) <- result.artifacts) {
          val a0 = artifact(a)
          val ent = new ju.AbstractMap.SimpleEntry(a0, f)
          artifactFiles.add(ent)
        }

        val deps = new ju.ArrayList[coursierapi.Dependency]
        result
          .resolution
          .orderedDependencies
          .map(dependency)
          .foreach(deps.add)

        coursierapi.FetchResult.of(artifactFiles, deps)
    }
  }

  def doComplete(complete: coursierapi.Complete): coursierapi.CompleteResult = {

    val cache0 = cache(complete.getCache)

    val repositories = complete
      .getRepositories
      .asScala
      .map(repository)
      .toVector

    val res = coursier.complete.Complete(cache0)
      .withRepositories(repositories)
      .withScalaBinaryVersionOpt(Option(complete.getScalaBinaryVersion))
      .withScalaVersionOpt(Option(complete.getScalaVersion))
      .withInput(complete.getInput)
      .complete()
      .unsafeRun()(cache0.ec)

    // FIXME Exceptions should be translated from coursier.* stuff to coursierapi.error.* ones

    coursierapi.CompleteResult.of(res._1, JavaConverters.seqAsJavaList(res._2))
  }

  def versionListing(versions: coursier.core.Versions): coursierapi.VersionListing =
    coursierapi.VersionListing.of(
      versions.latest,
      versions.release,
      JavaConverters.seqAsJavaList(versions.available),
      versions
        .lastUpdated
        .map { dt =>
          LocalDateTime.of(
            dt.year,
            dt.month,
            dt.day,
            dt.hour,
            dt.minute,
            dt.second
          )
        }
        .orNull
    )

  def versionListing(versions: coursierapi.VersionListing): coursier.core.Versions =
    coursier.core.Versions(
      versions.getLatest,
      versions.getRelease,
      versions.getAvailable.asScala.toList,
      Option(versions.getLastUpdated).map { dt =>
        coursier.core.Versions.DateTime(
          dt.getYear,
          dt.getMonthValue,
          dt.getDayOfMonth,
          dt.getHour,
          dt.getMinute,
          dt.getSecond
        )
      }
    )

  def versions(versions: coursierapi.Versions): coursier.Versions[Task] = {

    val cache0 = cache(versions.getCache)

    val repositories = versions
      .getRepositories
      .asScala
      .map(repository)
      .toVector

    coursier.Versions(cache0)
      .withRepositories(repositories)
      .withModule(module(versions.getModule))
  }

  def versions(versions: coursier.Versions[Task]): coursierapi.Versions = {

    val cache0 = cache(
      versions.cache match {
        case f: FileCache[Task] => f
        case c => sys.error(s"Unsupported cache type: $c")
      }
    )

    coursierapi.Versions.create()
      .withCache(cache0)
      .withRepositories(versions.repositories.map(repository): _*)
      .withModule(versions.moduleOpt.map(module).orNull)
  }

  def getVersions(versions0: coursierapi.Versions): coursierapi.VersionsResult = {

    val ver = versions(versions0)
    val res = ver.result().unsafeRun()(ver.cache.ec)

    // FIXME Exceptions should be translated from coursier.* stuff to coursierapi.error.* ones

    val errors = res.results.collect {
      case (repo, Left(error)) =>
        new ju.AbstractMap.SimpleImmutableEntry(repository(repo), error): ju.Map.Entry[coursierapi.Repository, String]
    }

    val listings = res.results.collect {
      case (repo, Right(ver)) =>
        new ju.AbstractMap.SimpleImmutableEntry(repository(repo), versionListing(ver)): ju.Map.Entry[coursierapi.Repository, coursierapi.VersionListing]
    }

    coursierapi.VersionsResult.of(
      JavaConverters.seqAsJavaList(errors),
      JavaConverters.seqAsJavaList(listings),
      versionListing(res.versions)
    )
  }

  def cacheGet(cache: coursierapi.Cache, artifact: coursierapi.Artifact): File = {
    val cache0 = ApiHelper.cache(cache)
    cache0.file(ApiHelper.artifact(artifact)).run.unsafeRun()(cache0.ec) match {
      case Left(err) => throw err
      case Right(f) => f
    }
  }

  def archiveCache(archiveCache: coursierapi.ArchiveCache): ArchiveCache[Task] =
    ArchiveCache(archiveCache.getLocation, cache(archiveCache.getCache), UnArchiver.default())

  def archiveCacheGet(archiveCache: coursierapi.ArchiveCache, artifact: coursierapi.Artifact): File = {
    val archiveCache0 = ApiHelper.archiveCache(archiveCache)
    archiveCache0.get(ApiHelper.artifact(artifact)).unsafeRun()(archiveCache0.cache.ec) match {
      case Left(err) => throw err
      case Right(f) => f
    }
  }

  def archiveCacheGetIfExists(archiveCache: coursierapi.ArchiveCache, artifact: coursierapi.Artifact): File = {
    val archiveCache0 = ApiHelper.archiveCache(archiveCache)
    archiveCache0.getIfExists(ApiHelper.artifact(artifact)).unsafeRun()(archiveCache0.cache.ec) match {
      case Left(err) => throw err
      case Right(f) => f.orNull
    }
  }

  def jvmManagerGet(manager: coursierapi.JvmManager, jvmId: String): File = {

    val jvmCache = JvmCache()
      .withDefaultIndex
      .withArchiveCache(archiveCache(manager.getArchiveCache))
    val javaHome = JavaHome().withCache(jvmCache)

    javaHome.get(jvmId).unsafeRun()(jvmCache.archiveCache.cache.ec)
  }

  def compareVersions(version0: String, version1: String): Int =
    Version(version0).compareTo(Version(version1))

}
