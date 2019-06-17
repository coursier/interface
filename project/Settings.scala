
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._
import sbt.Keys._

object Settings {

  def scala212 = "2.12.8"

  lazy val shared = Seq(
    scalaVersion := scala212,
    crossScalaVersions := Seq(scala212),
    scalacOptions += "-target:jvm-1.8",
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8"
    )
  )

  lazy val mima = Seq(
    MimaPlugin.autoImport.mimaPreviousArtifacts := {
      Mima.binaryCompatibilityVersions.map { ver =>
        (organization.value % moduleName.value % ver).cross(crossVersion.value)
      }
    }
  )

}
