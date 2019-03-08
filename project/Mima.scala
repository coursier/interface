
import sys.process._

object Mima {

  private def stable(ver: String): Boolean =
    ver.exists(c => c != '0' && c != '.') &&
    ver
      .replace("-RC", "-")
      .forall(c => c == '.' || c == '-' || c.isDigit)

  def binaryCompatibilityVersions: Set[String] = {

    val latest = Seq("git", "describe", "--tags", "--abbrev=0", "--match", "v*")
      .!!
      .trim
      .stripPrefix("v")

    assert(latest.nonEmpty, "Could not find latest version")

    if (stable(latest)) {
      val prefix = latest.split('.').take(2).map(_ + ".").mkString

      val previous = Seq("git", "tag", "--list", "v" + prefix + "*")
        .!!
        .linesIterator
        .map(_.trim.stripPrefix("v"))
        .filter(stable)
        .toSet

      assert(previous.contains(latest), "Something went wrong")

      previous
    } else
      Set()
  }
}
