package coursierapi

import scala.collection.JavaConverters._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object Interpolators {

  object Macros {

    private def is211: Boolean =
      scala.util.Properties.versionNumberString.startsWith("2.11.")

    def safeModule(c: blackbox.Context)(args: c.Expr[Any]*): c.Expr[Module] = {
      import c.universe._
      c.prefix.tree match {
        case Apply(_, List(Apply(_, Literal(Constant(modString: String)) :: Nil))) =>
          val mod = coursierapi.Module.parse(modString, coursierapi.ScalaVersion.of(scala.util.Properties.versionNumberString))
              val attrs = mod.getAttributes.asScala.toSeq.map {
                case (k, v) =>
                  q"_root_.scala.Tuple2($k, $v)"
              }
          if (is211)
            c.Expr(q"""
              _root_.coursierapi.Module.of(
                ${mod.getOrganization},
                ${mod.getName},
                _root_.scala.collection.JavaConversions.mapAsJavaMap(_root_.scala.collection.immutable.Map(..$attrs))
              )
            """)
          else
            c.Expr(q"""
              _root_.coursierapi.Module.of(
                ${mod.getOrganization},
                ${mod.getName},
                _root_.scala.collection.JavaConverters.mapAsJavaMap(_root_.scala.collection.immutable.Map(..$attrs))
              )
            """)
        case _ =>
          c.abort(c.enclosingPosition, s"Only a single String literal is allowed here")
      }
    }

    def safeDependency(c: blackbox.Context)(args: c.Expr[Any]*): c.Expr[Dependency] = {
      import c.universe._
      c.prefix.tree match {
        case Apply(_, List(Apply(_, Literal(Constant(modString: String)) :: Nil))) =>
          val dep = coursierapi.Dependency.parse(modString, ScalaVersion.of(scala.util.Properties.versionNumberString))
          val attrs = dep.getModule.getAttributes.asScala.toSeq.map {
            case (k, v) =>
              q"_root_.scala.Tuple2($k, $v)"
          }
          if (is211)
            c.Expr(q"""
              _root_.coursierapi.Dependency.of(
                _root_.coursierapi.Module.of(
                  ${dep.getModule.getOrganization},
                  ${dep.getModule.getName},
                  _root_.scala.collection.JavaConversions.mapAsJavaMap(_root_.scala.collection.immutable.Map(..$attrs))
                ),
                ${dep.getVersion}
              )
            """)
          else
            c.Expr(q"""
              _root_.coursierapi.Dependency.of(
                _root_.coursierapi.Module.of(
                  ${dep.getModule.getOrganization},
                  ${dep.getModule.getName},
                  _root_.scala.collection.JavaConverters.mapAsJavaMap(_root_.scala.collection.immutable.Map(..$attrs))
                ),
                ${dep.getVersion}
              )
            """)
        case _ =>
          c.abort(c.enclosingPosition, s"Only a single String literal is allowed here")
      }
    }

    def safeMavenRepository(c: blackbox.Context)(args: c.Expr[Any]*): c.Expr[MavenRepository] = {
      import c.universe._
      c.prefix.tree match {
        case Apply(_, List(Apply(_, Literal(Constant(root: String)) :: Nil))) =>
          // FIXME Check that there's no query string, fragment, … in uri?
          val uri = new java.net.URI(root)
          c.Expr(q"""_root_.coursierapi.MavenRepository.of($root)""")
        case _ =>
          c.abort(c.enclosingPosition, s"Only a single String literal is allowed here")
      }
    }

    def safeIvyRepository(c: blackbox.Context)(args: c.Expr[Any]*): c.Expr[IvyRepository] = {
      import c.universe._
      c.prefix.tree match {
        case Apply(_, List(Apply(_, Literal(Constant(str: String)) :: Nil))) =>
          // FIXME Check that there's no query string, fragment, … in uri?
          val r = coursierapi.IvyRepository.of(str)
          // Here, ideally, we should lift r as an Expr, but this is quite cumbersome to do (it involves lifting
          // Seq[coursier.ivy.Pattern.Chunk], where coursier.ivy.Pattern.Chunk is an ADT, …
          c.Expr(q"""_root_.coursierapi.IvyRepository.of($str)""")
        case _ =>
          c.abort(c.enclosingPosition, s"Only a single String literal is allowed here")
      }
    }
  }

  implicit class moduleString(val sc: StringContext) extends AnyVal {
    def mod(args: Any*): Module = macro Macros.safeModule
  }

  implicit class dependencyString(val sc: StringContext) extends AnyVal {
    def dep(args: Any*): Dependency = macro Macros.safeDependency
  }

  implicit class mavenRepositoryString(val sc: StringContext) extends AnyVal {
    def mvn(args: Any*): MavenRepository = macro Macros.safeMavenRepository
  }

  implicit class ivyRepositoryString(val sc: StringContext) extends AnyVal {
    def ivy(args: Any*): IvyRepository = macro Macros.safeIvyRepository
  }

}
