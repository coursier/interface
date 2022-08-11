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
      import com.eed3si9n.jarjar._
      import com.eed3si9n.jarjar.util.StandaloneJarProcessor

      val orig = (Proguard / proguard).value.head
      val origLastModified = orig.lastModified()
      val dest = orig.getParentFile / s"${orig.getName.stripSuffix(".jar")}-with-renaming-test.jar"
      if (!dest.exists() || dest.lastModified() < origLastModified) {
        val tmpDest = orig.getParentFile / s"${orig.getName.stripSuffix(".jar")}-with-renaming-test-0.jar"
        val tmpDest1 = orig.getParentFile / s"${orig.getName.stripSuffix(".jar")}-with-renaming-test-1.jar"

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
          rename("concurrentrefhashmap.**", "coursierapi.shaded.concurrentrefhashmap.@1"),
          rename("org.apache.commons.compress.**", "coursierapi.shaded.commonscompress.@1"),
          rename("org.apache.commons.io.input.**", "coursierapi.shaded.commonsio.@1"),
          rename("org.codehaus.plexus.**", "coursierapi.shaded.plexus.@1"),
          rename("org.tukaani.xz.**", "coursierapi.shaded.xz.@1"),
          rename("org.iq80.snappy.**", "coursierapi.shaded.snappy.@1"),
          rename("com.github.plokhotnyuk.jsoniter_scala.core.**", "coursierapi.shaded.jsoniter.@1")
        )

        val processor = new com.eed3si9n.jarjar.JJProcessor(
          rules,
          verbose = false,
          skipManifest = true,
          misplacedClassStrategy = "fatal"
        )
        StandaloneJarProcessor.run(orig, tmpDest, processor)

        val toBeRemoved = Set(
          "LICENSE",
          "NOTICE",
          "README"
        )
        val directoriesToBeRemoved = Seq(
          "licenses/"
        )
        assert(directoriesToBeRemoved.forall(_.endsWith("/")))
        ZipUtil.removeFromZip(
          tmpDest,
          tmpDest1,
          name =>
            toBeRemoved(name) || directoriesToBeRemoved.exists(dir =>
              name.startsWith(dir)
            )
        )
        tmpDest.delete()

        val serviceContent =
          ZipUtil.zipEntryContent(orig, "META-INF/services/coursier.jniutils.NativeApi").getOrElse {
            sys.error(s"META-INF/services/coursier.jniutils.NativeApi not found in $orig")
          }

        ZipUtil.addToZip(
          tmpDest1,
          dest,
          Seq(
            "META-INF/services/coursierapi.shaded.coursier.jniutils.NativeApi" -> serviceContent
          )
        )

        tmpDest1.delete()
      }
      Check.onlyNamespace("coursierapi", dest)
      dest
    },
    addArtifact(Compile / packageBin / artifact, finalPackageBin),
    Proguard / proguardVersion := "7.2.2",
    Proguard / proguardOptions ++= {
      val baseOptions = Seq(
        "-dontnote",
        "-dontwarn",
        "-dontobfuscate",
        "-dontoptimize",
        "-keep class coursierapi.** {\n  public protected *;\n}"
      )

      val isJava9OrMore = sys.props.get("java.version").exists(!_.startsWith("1."))
      val maybeJava9Options =
        if (isJava9OrMore) {
          val javaHome = sys.props.getOrElse("java.home", ???)
          Seq(s"-libraryjars $javaHome/jmods/java.base.jmod")
        }
        else
          Nil

      baseOptions ++ maybeJava9Options
    },
    Proguard / proguard / javaOptions := Seq("-Xmx3172M"),

    // Adding the interface JAR rather than its classes directory.
    // The former contains META-INF stuff in particular.
    Proguard / proguardInputs := (Proguard / proguardInputs).value.filter(f => !f.isDirectory || f.getName != "classes"),
    Proguard / proguardInputs += (Compile / packageBin).value,

    Proguard / proguardBinaryDeps := Settings.getAllBinaryDeps.value,
    Proguard / proguardBinaryDeps ++= Settings.rtJarOpt.toSeq, // seems needed with sbt 1.4.0

    Proguard / proguardInputFilter := { file =>
      file.name match {
        case n if n.startsWith("interface") => None // keep META-INF from main JAR
        case n if n.startsWith("windows-jni-utils") => Some("!META-INF/MANIFEST.MF")
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
    libraryDependencies ++= Seq(
      ("io.get-coursier" %% "coursier" % "2.1.0-M6-49-gff26f8e39")
        .exclude("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros_2.12")
        .exclude("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros_2.13"),
      ("io.get-coursier" %% "coursier-jvm" % "2.1.0-M6-49-gff26f8e39")
        .exclude("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros_2.12")
        .exclude("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros_2.13"),
      "io.get-coursier.jniutils" % "windows-jni-utils-coursierapi" % "0.3.2"
    ),

    libraryDependencies += "com.lihaoyi" %% "utest" % "0.8.0" % Test,
    testFrameworks += new TestFramework("utest.runner.Framework"),

    mimaBinaryIssueFilters ++= Seq(
      // users shouldn't ever reference those
      ProblemFilters.exclude[Problem]("coursierapi.shaded.*"),
      ProblemFilters.exclude[Problem]("coursierapi.internal.*")
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

lazy val `interface-svm-subs` = project
  .disablePlugins(MimaPlugin)
  .dependsOn(interface)
  .settings(
    Settings.shared,
    libraryDependencies += "org.graalvm.nativeimage" % "svm" % "22.0.0.2" % Provided,
    autoScalaLibrary := false,
    crossVersion := CrossVersion.disabled,
    // we don't actually depend on that thanks to proguarding / shading in interface
    dependencyOverrides += "org.scala-lang" % "scala-library" % scalaVersion.value
  )

lazy val interpolators = project
  .dependsOn(interface)
  .settings(
    Settings.shared,
    Settings.mima(no213 = true),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "com.lihaoyi" %% "utest" % "0.8.0" % Test
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
    publish / skip := true,
    crossPaths := false, // https://github.com/sbt/junit-interface/issues/35
    autoScalaLibrary := false,
    crossVersion := CrossVersion.disabled,
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.13.2" % Test,
      "com.github.sbt" % "junit-interface" % "0.13.3" % Test
    ),
    libraryDependencies ++= {
      val org = (interface / organization).value
      val name = (interface / moduleName).value
      sys.env.get("TEST_VERSION").toSeq.map { v =>
        org % name % v
      }
    },
    Test / unmanagedClasspath ++= Def.taskDyn {
      if (sys.env.get("TEST_VERSION").isEmpty)
        Def.task {
          Seq((interface / finalPackageBin).value)
	}
      else
        Def.task(Seq.empty[File])
    }.value
  )

publish / skip := true
Settings.shared
disablePlugins(MimaPlugin)
