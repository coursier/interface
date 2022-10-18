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
        !n.startsWith("META-INF/") && !n.startsWith(ns + "/") &&
          n != "reflect.properties" && // scala-reflect adds that
          n != "scala-collection-compat.properties" && // collection-compat adds that
          !n.contains("/libzstd-jni-") // com.github.luben:zstd-jni stuff (pulled via plexus-archiver)
      }
      .toVector
      .sorted
    for (u <- unrecognized)
      System.err.println(s"Unrecognized: $u")
    assert(unrecognized.isEmpty)
  }

}
