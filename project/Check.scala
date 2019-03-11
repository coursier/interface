import java.io.File
import java.util.zip.ZipFile

import scala.collection.JavaConverters._

object Check {

  def onlyNamespace(ns: String, jar: File): Unit = {
    val zf = new ZipFile(jar)
    val unrecognized = zf.entries()
      .asScala
      .map(_.getName)
      .filter { n =>
        !n.startsWith("META-INF/") && !n.startsWith(ns + "/")
      }
      .toVector
      .sorted
    for (u <- unrecognized)
      System.err.println(s"Unrecognized: $u")
    assert(unrecognized.isEmpty)
  }

}
