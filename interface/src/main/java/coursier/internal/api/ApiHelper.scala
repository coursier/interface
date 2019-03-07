package coursier.internal.api

import java.io.{File, OutputStreamWriter}
import java.util.concurrent.ExecutorService

import coursier._
import coursier.api.{Logger, SimpleLogger}
import coursier.cache.loggers.RefreshLogger
import coursier.cache.{CacheDefaults, CacheLogger, FileCache}
import coursier.util.Task

import scala.collection.JavaConverters._

object ApiHelper {

  private final case class ApiRepo(repo: Repository) extends coursier.api.Repository

  def defaultRepositories(): Array[coursier.api.Repository] =
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

  def fetch(fetch: coursier.api.Fetch): Fetch[Task] = {

    val dependencies = fetch
      .getDependencies
      .asScala
      .map { jDep =>
        Dependency(
          Module(
            Organization(jDep.getModule.getOrganization),
            ModuleName(jDep.getModule.getName),
            jDep.getModule.getAttributes.asScala.iterator.toMap
          ),
          jDep.getVersion
        )
      }

    val repositories = fetch
      .getRepositories
      .asScala
      .map {
        case ApiRepo(repo) => repo
        case mvn: coursier.api.MavenRepository =>
          MavenRepository(mvn.getBase)
        case _ =>
          ???
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

    Fetch()
      .withDependencies(dependencies)
      .withRepositories(repositories)
      .withCache(cache)
      .withMainArtifacts(fetch.getMainArtifacts)
  }

  def doFetch(fetch: coursier.api.Fetch): Array[File] =
    ApiHelper.fetch(fetch)
      .run()
      .toArray

}
