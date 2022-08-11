
import java.io.{ByteArrayOutputStream, File, FileInputStream, FileOutputStream, InputStream}
import java.util.zip.{ZipEntry, ZipFile, ZipInputStream, ZipOutputStream}

object ZipUtil {

  private def readFullySync(is: InputStream) = {
    val buffer = new ByteArrayOutputStream
    val data = Array.ofDim[Byte](16384)

    var nRead = is.read(data, 0, data.length)
    while (nRead != -1) {
      buffer.write(data, 0, nRead)
      nRead = is.read(data, 0, data.length)
    }

    buffer.flush()
    buffer.toByteArray
  }

  private def zipEntries(zipStream: ZipInputStream): Iterator[(ZipEntry, Array[Byte])] =
    new Iterator[(ZipEntry, Array[Byte])] {
      private var nextEntry = Option.empty[ZipEntry]
      private def update() =
        nextEntry = Option(zipStream.getNextEntry)

      update()

      def hasNext = nextEntry.nonEmpty
      def next() = {
        val ent = nextEntry.get
        val data = readFullySync(zipStream)

        update()

        (ent, data)
      }
    }

  def removeFromZip(sourceZip: File, destZip: File, remove: String => Boolean): Unit = {

    val is = new FileInputStream(sourceZip)
    val os = new FileOutputStream(destZip)
    val bootstrapZip = new ZipInputStream(is)
    val outputZip = new ZipOutputStream(os)

    for ((ent, data) <- zipEntries(bootstrapZip) if !remove(ent.getName)) {

      // Same workaround as https://github.com/spring-projects/spring-boot/issues/13720
      // (https://github.com/spring-projects/spring-boot/commit/a50646b7cc3ad941e748dfb450077e3a73706205#diff-2ff64cd06c0b25857e3e0dfdb6733174R144)
      ent.setCompressedSize(-1L)

      outputZip.putNextEntry(ent)
      outputZip.write(data)
      outputZip.closeEntry()
    }

    outputZip.finish()
    outputZip.close()

    is.close()
    os.close()

  }

  def addOrOverwriteInZip(sourceZip: File, destZip: File, toAdd: Seq[(String, Array[Byte])]): Unit = {

    val is = new FileInputStream(sourceZip)
    val os = new FileOutputStream(destZip)
    val bootstrapZip = new ZipInputStream(is)
    val outputZip = new ZipOutputStream(os)

    val strip = toAdd.map(_._1).toSet

    for ((ent, data) <- zipEntries(bootstrapZip) if !strip.contains(ent.getName)) {

      // Same workaround as https://github.com/spring-projects/spring-boot/issues/13720
      // (https://github.com/spring-projects/spring-boot/commit/a50646b7cc3ad941e748dfb450077e3a73706205#diff-2ff64cd06c0b25857e3e0dfdb6733174R144)
      ent.setCompressedSize(-1L)

      outputZip.putNextEntry(ent)
      outputZip.write(data)
      outputZip.closeEntry()
    }

    for ((name, content) <- toAdd) {
      val ent = new ZipEntry(name)
      outputZip.putNextEntry(ent)
      outputZip.write(content)
      outputZip.closeEntry()
    }

    outputZip.finish()
    outputZip.close()

    is.close()
    os.close()

  }

  def zipEntryContent(sourceZip: File, entryName: String): Option[Array[Byte]] = {
    val zf = new ZipFile(sourceZip)
    val entryOpt = Option(zf.getEntry(entryName))
    val content = entryOpt.map { entry =>
      readFullySync(zf.getInputStream(entry))
    }
    zf.close()
    content
  }

}
