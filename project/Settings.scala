
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._
import sbt.Keys._

import scala.util.Properties

import java.util.Locale

object Settings {

  def scala213 = "2.13.15"

  def scala212 = "2.12.20"

  private def isArm64 =
    Option(System.getProperty("os.arch")).map(_.toLowerCase(Locale.ROOT)) match {
      case Some("aarch64" | "arm64") => true
      case _                         => false
    }
  private lazy val java8Home = Option(System.getenv("COURSIER_INTERFACE_JAVA8_HOME")).getOrElse {
    val jvmId =
      if (Properties.isMac && isArm64)
        // no native JDK 8 on Mac ARM, using amd64 one
        "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u432-b06/OpenJDK8U-jdk_x64_mac_hotspot_8u432b06.tar.gz"
      else
        "adoptium:8"
    os.proc("cs", "java-home", "--jvm", jvmId)
      .call()
      .out.trim()
  }
  private lazy val rtJar = {
    val path = os.Path(java8Home) / "jre/lib/rt.jar"
    assert(os.isFile(path))
    path
  }
  lazy val shared = Seq(
    scalaVersion := scala213,
    crossScalaVersions := Seq(scala213, scala212),
    scalacOptions ++= Seq("--release", "8"),
    javacOptions ++= Seq(
      "-source", "8",
      "-target", "8",
      "-bootclasspath", rtJar.toString
    ),
    Compile / doc / javacOptions := Seq(
      "-source", "8",
      "-bootclasspath", rtJar.toString
    )
  )

  private val filterOut = Set("0.0.1")
  private def no212Versions = (0 to 14).map("0.0." + _).toSet
  def mima(no213: Boolean = false) = Seq(
    MimaPlugin.autoImport.mimaPreviousArtifacts := {
      val sv = scalaVersion.value
      val is212 = sv.startsWith("2.12.")
      val is213 = sv.startsWith("2.13.")
      Mima.binaryCompatibilityVersions
        .filter(v => !filterOut(v))
        .filter(v => (!is213 || !no213) && (!is212 || !no212Versions(v)))
        .map { ver =>
          (organization.value % moduleName.value % ver)
            .cross(crossVersion.value)
        }
    }
  )

  // https://github.com/sbt/sbt-proguard/blob/2c502f961245a18677ef2af4220a39e7edf2f996/src/main/scala-sbt-1.0/com/typesafe/sbt/proguard/Sbt10Compat.scala#L8-L13
  // but sbt 1.4-compatible
  val getAllBinaryDeps: Def.Initialize[Task[Seq[java.io.File]]] = Def.task {
    import sbt.internal.inc.Analysis
    val converter = fileConverter.value
    (Compile / compile).value match {
      case analysis: Analysis =>
        analysis.relations.allLibraryDeps.toSeq.map(converter.toPath(_).toFile)
    }
  }

  lazy val rtJarOpt = sys.props.get("sun.boot.class.path")
    .toSeq
    .flatMap(_.split(java.io.File.pathSeparator).toSeq)
    .map(java.nio.file.Paths.get(_))
    .find(_.endsWith("rt.jar"))
    .map(_.toFile)

}
