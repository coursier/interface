import com.typesafe.tools.mima.core.{MissingClassProblem, Problem, ProblemFilters}

inThisBuild(List(
  organization := "io.get-coursier",
  homepage := Some(url("https://github.com/coursier/interface")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "alexarchambault",
      "Alexandre Archambault",
      "alexandre.archambault@gmail.com",
      url("https://github.com/alexarchambault")
    )
  )
))

val shadingNamespace0 = "coursierapi.shaded"

lazy val interface = project
  .enablePlugins(ShadingPlugin)
  .settings(
    Settings.shared,
    // shading stuff
    publish := publish.in(Shading).value,
    publishLocal := publishLocal.in(Shading).value,
    inConfig(_root_.coursier.ShadingPlugin.Shading)(com.typesafe.sbt.pgp.PgpSettings.projectSettings),
    ShadingPlugin.projectSettings, // seems this has to be repeated, *after* the addition of PgpSettings…
    PgpKeys.publishSigned := PgpKeys.publishSigned.in(Shading).value,
    PgpKeys.publishLocalSigned := PgpKeys.publishLocalSigned.in(Shading).value,
    shadingNamespace := shadingNamespace0,
    shadeNamespaces ++= Set(
      "coursier",
      "io.github.soc.directories",
      "scala"
    ),

    autoScalaLibrary := false,
    libraryDependencies += "io.get-coursier" %% "coursier" % "1.1.0-M13-1" % "shaded",

    mimaBinaryIssueFilters ++= Seq(
      // users shouln't ever reference this
      ProblemFilters.exclude[Problem](s"$shadingNamespace0.*"),
    )

  )

lazy val interpolators = project
  .dependsOn(interface)
  .settings(
    Settings.shared,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "com.lihaoyi" %% "utest" % "0.6.6" % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),

    mimaBinaryIssueFilters ++= Seq(
      // only used at compile time, not runtime
      ProblemFilters.exclude[MissingClassProblem]("coursierapi.Interpolators$Macros$"),
    )
  )

lazy val `coursier-interface` = project
  .in(file("."))
  .aggregate(interface, interpolators)
  .settings(
    publishArtifact := false
  )
