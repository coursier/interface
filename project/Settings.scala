
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._
import sbt.Keys._

object Settings {

  def scala213 = "2.13.11"
  def scala212 = "2.12.18"

  lazy val shared = Seq(
    scalaVersion := scala213,
    crossScalaVersions := Seq(scala213, scala212),
    scalacOptions += "-target:jvm-1.8",
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8"
    ),
    Compile / doc / javacOptions := Seq("-source", "1.8")
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
