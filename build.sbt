import com.tonicsystems.jarjar.classpath.ClassPath
import com.tonicsystems.jarjar.transform.JarTransformer
import com.tonicsystems.jarjar.transform.config.ClassRename
import com.tonicsystems.jarjar.transform.jar.DefaultJarProcessor
import com.typesafe.tools.mima.core.{MissingClassProblem, Problem, ProblemFilters}

import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}
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
    skip.in(publish) := scalaVersion.value != Settings.scala212,
    finalPackageBin := {
      val log = streams.value.log
      val orig = proguard.in(Proguard).value.head
      val origLastModified = orig.lastModified()
      val dest = orig.getParentFile / s"${orig.getName.stripSuffix(".jar")}-with-renaming.jar"
      if (!dest.exists() || dest.lastModified() < origLastModified) {

        val processor = new DefaultJarProcessor

        processor.addClassRename(new ClassRename("scala.**", "coursierapi.shaded.scala.@1"))
        processor.addClassRename(new ClassRename("coursier.**", "coursierapi.shaded.coursier.@1"))
        processor.addClassRename(new ClassRename("io.github.soc.directories.**", "coursierapi.shaded.directories.@1"))

        val transformer = new JarTransformer(dest, processor)
        val cp = new ClassPath(file(sys.props("user.dir")), Array(orig))

        log.info(s"Generating $dest")
        transformer.transform(cp)

        Check.onlyNamespace("coursierapi", dest)

        dest.setLastModified(origLastModified)
      }
      dest
    },
    addArtifact(artifact.in(Compile, packageBin), finalPackageBin),
    proguardOptions.in(Proguard) ++= Seq(
      "-dontwarn",
      "-dontobfuscate",
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
    libraryDependencies += "io.get-coursier" %% "coursier" % "1.1.0-M14-1",

    libraryDependencies += "com.lihaoyi" %% "utest" % "0.6.7" % Test,
    testFrameworks += new TestFramework("utest.runner.Framework"),

    mimaBinaryIssueFilters ++= Seq(
      // users shouln't ever reference those
      ProblemFilters.exclude[Problem]("coursierapi.shaded.*"),
    ),

    autoScalaLibrary := false,
    crossVersion := CrossVersion.disabled,

    // filtering out non cross versioned module in 0.0.1 (published cross-versioned there, added below)
    mimaPreviousArtifacts := mimaPreviousArtifacts.value.filter(_.revision != "0.0.1"),

    // was cross-versioned publishing in 0.0.1
    mimaPreviousArtifacts += organization.value %% "interface" % "0.0.1",

  )

lazy val interpolators = project
  .dependsOn(interface)
  .settings(
    Settings.shared,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
      "com.lihaoyi" %% "utest" % "0.6.7" % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),

    mimaBinaryIssueFilters ++= Seq(
      // only used at compile time, not runtime
      ProblemFilters.exclude[MissingClassProblem]("coursierapi.Interpolators$Macros$"),
    )
  )

skip.in(publish) := true
