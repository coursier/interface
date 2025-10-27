addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")
addSbtPlugin("com.github.sbt" % "sbt-proguard" % "0.5.0")

libraryDependencies += "com.eed3si9n.jarjarabrams" %% "jarjar-abrams-core" % "1.16.0"

libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.11.5"
