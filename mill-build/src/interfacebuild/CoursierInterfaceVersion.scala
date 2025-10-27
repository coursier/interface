package interfacebuild

import mill.*
import mill.api.*

object CoursierInterfaceVersion extends ExternalModule {

  def latestTaggedVersion(): String =
    os.proc("git", "describe", "--abbrev=0", "--tags", "--match", "v*")
      .call().out
      .trim()

  def computeBuildVersion(): String = {
    val gitHead = os.proc("git", "rev-parse", "HEAD").call().out.trim()
    val maybeExactTag = {
      val res = os.proc("git", "describe", "--exact-match", "--tags", "--always", gitHead)
        .call(stderr = os.Pipe, check = false)
      if (res.exitCode == 0)
        Some(res.out.trim().stripPrefix("v"))
      else
        None
    }
    maybeExactTag.getOrElse {
      val latestTaggedVersion0 = latestTaggedVersion()
      val commitsSinceTaggedVersion =
        os.proc("git", "rev-list", gitHead, "--not", latestTaggedVersion0, "--count")
          .call().out.trim()
          .toInt
      val gitHash = os.proc("git", "rev-parse", "--short", "HEAD").call().out.trim()
      s"${latestTaggedVersion0.stripPrefix("v")}-$commitsSinceTaggedVersion-$gitHash-SNAPSHOT"
    }
  }
  def buildVersion: T[String] = Task.Input {
    computeBuildVersion()
  }

  lazy val millDiscover: Discover = Discover[this.type]
}
