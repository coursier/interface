
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._
import sbt.Keys._

object Settings {

  def scala213 = "2.13.1"
  def scala212 = "2.12.10"

  lazy val shared = Seq(
    scalaVersion := scala213,
    crossScalaVersions := Seq(scala213, scala212),
    scalacOptions += "-target:jvm-1.8",
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8"
    )
  )

  private val filterOut = Set("0.0.1")
  private def no213Versions = (0 to 8).map("0.0." + _).toSet
  def mima(no213: Boolean = false) = Seq(
    MimaPlugin.autoImport.mimaPreviousArtifacts := {
      val sv = scalaVersion.value
      val is213 = sv.startsWith("2.13.")
      Mima.binaryCompatibilityVersions
        .filter(v => !filterOut(v))
        .filter(v => !is213 || !no213 || !no213Versions(v))
        .map { ver =>
          (organization.value % moduleName.value % ver)
            .cross(crossVersion.value)
        }
    }
  )

}
