package com.github.lolgab.mill.mima

import mill.javalib.Dep
import mill.api.*
import mill.*

import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import scala.jdk.CollectionConverters.*
import scala.util.Using

trait InterfaceMima extends Mima {
  def resolvedMimaPreviousArtifacts: T[Seq[(Dep, PathRef)]] = Task {
    val base = super.resolvedMimaPreviousArtifacts()
    val dir = Task.dest
    base.zipWithIndex.map {
      case ((dep, ref), idx) =>
        val dest = dir / s"$idx.jar"
        InterfaceMima.cleanUpJar(
          ref.path,
          dest,
          ignore = name => !name.startsWith("coursierapi/shaded/") || name.startsWith("coursierapi/internal/")
        )
        (dep, PathRef(dest))
    }
  }
}

object InterfaceMima {
  def cleanUpJar(input: os.Path, output: os.Path, ignore: String => Boolean): Unit =
    Using.resources(
      new ZipFile(input.toIO),
      os.write.outputStream(output)
    ) { (zf, os0) =>
      val zos = new ZipOutputStream(os0)
      def keep(name: String) =
        !name.startsWith("coursierapi/shaded/") &&
          !name.startsWith("coursierapi/internal/")
      for (ent <- zf.entries().asScala if keep(ent.getName)) {
        val ent0 = new ZipEntry(ent)
        zos.putNextEntry(ent0)
        val entryContent = zf.getInputStream(ent).readAllBytes()
        zos.write(entryContent)
        zos.closeEntry()
      }
      zos.finish()
    }
}
