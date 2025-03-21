addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.9.3")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")
addSbtPlugin("com.lightbend.sbt" % "sbt-proguard" % "0.4.0")

libraryDependencies += "com.eed3si9n.jarjarabrams" %% "jarjar-abrams-core" % "1.14.1"

libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.11.4"
