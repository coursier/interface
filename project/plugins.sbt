addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.3")
addSbtPlugin(("io.github.alexarchambault.sbt" % "sbt-compatibility" % "0.0.6").exclude("com.typesafe", "sbt-mima-plugin"))
addSbtPlugin("com.github.alexarchambault.tmp" % "sbt-mima-plugin" % "0.7.1-SNAPSHOT")
addSbtPlugin("com.lightbend.sbt" % "sbt-proguard" % "0.3.0")

resolvers += Resolver.sonatypeRepo("snapshots")
