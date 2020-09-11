import com.typesafe.tools.mima.core.{MissingClassProblem, Problem, ProblemFilters}

import scala.xml.{Node => XmlNode, _}
import scala.xml.transform.{RewriteRule, RuleTransformer}

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

lazy val finalPackageBin = taskKey[File]("")

lazy val interface = project
  .enablePlugins(SbtProguard)
  .settings(
    moduleName := {
      val former = moduleName.value
      val sv = scalaVersion.value
      val sbv = sv.split('.').take(2).mkString(".")
      if (sv == Settings.scala213)
        former
      else
        former + "-scala-" + sbv + "-shaded"
    },
    finalPackageBin := {
      import org.pantsbuild.jarjar._
      import org.pantsbuild.jarjar.util.StandaloneJarProcessor

      val orig = proguard.in(Proguard).value.head
      val origLastModified = orig.lastModified()
      val dest = orig.getParentFile / s"${orig.getName.stripSuffix(".jar")}-with-renaming-test.jar"
      if (!dest.exists() || dest.lastModified() < origLastModified) {
        val tmpDest = orig.getParentFile / s"${orig.getName.stripSuffix(".jar")}-with-renaming-test-0.jar"

        def rename(from: String, to: String): Rule = {
          val rule = new Rule
          rule.setPattern(from)
          rule.setResult(to)
          rule
        }

        val rules = Seq(
          rename("scala.**", "coursierapi.shaded.scala.@1"),
          rename("coursier.**", "coursierapi.shaded.coursier.@1"),
          rename("org.fusesource.**", "coursierapi.shaded.org.fusesource.@1"),
          rename("io.github.alexarchambault.windowsansi.**", "coursierapi.shaded.windowsansi.@1"),
        )

        val processor = new org.pantsbuild.jarjar.JJProcessor(
          rules,
          verbose = false,
          skipManifest = true,
          misplacedClassStrategy = "fatal"
        )
        StandaloneJarProcessor.run(orig, tmpDest, processor)

        ZipUtil.removeFromZip(tmpDest, dest, Set("LICENSE", "NOTICE"))
        tmpDest.delete()
      }
      Check.onlyNamespace("coursierapi", dest)
      dest
    },
    addArtifact(artifact.in(Compile, packageBin), finalPackageBin),
    proguardVersion.in(Proguard) := "7.0.0",
    proguardOptions.in(Proguard) ++= Seq(
      "-dontnote",
      "-dontwarn",
      "-dontobfuscate",
      "-dontoptimize",
      "-keep class coursierapi.** {\n  public protected *;\n}",
    ),
    javaOptions.in(Proguard, proguard) := Seq("-Xmx3172M"),

    // Adding the interface JAR rather than its classes directory.
    // The former contains META-INF stuff in particular.
    proguardInputs.in(Proguard) := proguardInputs.in(Proguard).value.filter(f => !f.isDirectory || f.getName != "classes"),
    proguardInputs.in(Proguard) += packageBin.in(Compile).value,

    proguardInputFilter.in(Proguard) := { file =>
      file.name match {
        case n if n.startsWith("interface") => None // keep META-INF from main JAR
        case n if n.startsWith("coursier-core") => Some("!META-INF/**,!coursier.properties,!coursier/coursier.properties")
        case n if n.startsWith("scala-xml") => Some("!META-INF/**,!scala-xml.properties")
        case n if n.startsWith("scala-library") => Some("!META-INF/**,!library.properties,!rootdoc.txt")
        case _ => Some("!META-INF/**")
      }
    },

    // inspired by https://github.com/olafurpg/coursier-small/blob/408528d10cea1694c536f55ba1b023e55af3e0b2/build.sbt#L44-L56
    pomPostProcess := { node =>
      new RuleTransformer(new RewriteRule {
        override def transform(node: XmlNode) = node match {
          case _: Elem if node.label == "dependency" =>
            val org = node.child.find(_.label == "groupId").fold("")(_.text.trim)
            val name = node.child.find(_.label == "artifactId").fold("")(_.text.trim)
            val ver = node.child.find(_.label == "version").fold("")(_.text.trim)
            Comment(s"shaded dependency $org:$name:$ver")
          case _ => node
        }
      }).transform(node).head
    },

    Settings.shared,
    Settings.mima(),
    libraryDependencies += "io.get-coursier" %% "coursier" % "2.0.0-RC6-26",

    libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.5" % Test,
    testFrameworks += new TestFramework("utest.runner.Framework"),

    mimaBinaryIssueFilters ++= Seq(
      // users shouln't ever reference those
      ProblemFilters.exclude[Problem]("coursierapi.shaded.*"),
    ),

    // clearing scalaModuleInfo in ivyModule, so that evicted doesn't
    // check scala versions
    ivyModule := {
      val is = ivySbt.value
      val config = moduleSettings.value match {
        case config0: ModuleDescriptorConfiguration =>
          config0.withScalaModuleInfo(None)
	case other => other
      }
      new is.Module(config)
    },
    autoScalaLibrary := false,
    crossVersion := CrossVersion.disabled,

    // filtering out non cross versioned module in 0.0.1 (published cross-versioned there, added below)
    mimaPreviousArtifacts := mimaPreviousArtifacts.value.filter(_.revision != "0.0.1"),

    // was cross-versioned publishing in 0.0.1
    mimaPreviousArtifacts ++= {
      val sv = scalaVersion.value
      // TODO When removing 2.12 support in the future, use org % interface_2.12 below?
      val is212 = sv.startsWith("2.12")
      if (is212)
        Set(organization.value %% "interface" % "0.0.1")
      else
        Set.empty[ModuleID]
    },

  )

lazy val interpolators = project
  .dependsOn(interface)
  .settings(
    Settings.shared,
    Settings.mima(no213 = true),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "com.lihaoyi" %% "utest" % "0.7.5" % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),

    mimaBinaryIssueFilters ++= Seq(
      // only used at compile time, not runtime
      ProblemFilters.exclude[MissingClassProblem]("coursierapi.Interpolators$Macros$"),
    )
  )

lazy val `interface-test` = project
  .disablePlugins(MimaPlugin)
  // .dependsOn(interface)
  .settings(
    Settings.shared,
    skip.in(publish) := true,
    autoScalaLibrary := false,
    crossVersion := CrossVersion.disabled,
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.13" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test
    ),
    libraryDependencies ++= {
      val org = organization.in(interface).value
      val name = moduleName.in(interface).value
      sys.env.get("TEST_VERSION").toSeq.map { v =>
        org % name % v
      }
    },
    unmanagedClasspath.in(Test) ++= Def.taskDyn {
      if (sys.env.get("TEST_VERSION").isEmpty)
        Def.task {
          Seq(finalPackageBin.in(interface).value)
	}
      else
        Def.task(Seq.empty[File])
    }.value
  )

skip.in(publish) := true
disablePlugins(MimaPlugin)
