addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.2.1")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-proguard" % "0.3.0")
addSbtCoursier

libraryDependencies += "io.get-coursier.jarjar" % "jarjar-core" % "1.0.1-coursier-1"
