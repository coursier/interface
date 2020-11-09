addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.4")
addSbtPlugin(("io.github.alexarchambault.sbt" % "sbt-compatibility" % "0.0.8").exclude("com.typesafe", "sbt-mima-plugin"))
addSbtPlugin("io.github.alexarchambault.sbt" % "sbt-eviction-rules" % "0.2.0")
addSbtPlugin("com.github.alexarchambault.tmp" % "sbt-mima-plugin" % "0.7.1-SNAPSHOT")
addSbtPlugin("com.lightbend.sbt" % "sbt-proguard" % "0.4.0")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies += "com.eed3si9n.jarjarabrams" %% "jarjar-abrams-core" % "0.3.0"
