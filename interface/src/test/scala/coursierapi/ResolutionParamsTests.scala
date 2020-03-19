package coursierapi

import coursier.internal.api.ApiHelper
import utest._

object ResolutionParamsTests extends TestSuite {

  val tests = Tests {
    'simple - {
      val params = ResolutionParams.create()
        .withMaxIterations(31)
        .forceVersion(Module.of("org", "foo"), "1.2")
        .forceVersion(Module.of("org", "bzz"), "1.3")
        .forceProperty("scala.version", "2.18.7")
      val params0 = ApiHelper.resolutionParams(ApiHelper.resolutionParams(params))

      assert(params != ResolutionParams.create())
      assert(params == params0)
    }

    'other - {
      * - {
        val params = ResolutionParams.create()
          .withMaxIterations(31)
          .forceVersion(Module.of("org", "foo"), "1.2")
          .forceVersion(Module.of("org", "bzz"), "1.3")
          .forceProperty("scala.version", "2.18.7")
          .addProfile("hadoop-foo")
          .addExclusion("org.scala-lang", "*")
          .addExclusion("org.scala-lang.modules", "*")
          .withUseSystemOsInfo(false)
          .withUseSystemJdkVersion(false)
        val params0 = ApiHelper.resolutionParams(ApiHelper.resolutionParams(params))

        assert(params != ResolutionParams.create())
        assert(params == params0)
      }

      * - {
        val params = ResolutionParams.create()
          .withMaxIterations(31)
          .forceVersion(Module.of("org", "foo"), "1.2")
          .forceVersion(Module.of("org", "bzz"), "1.3")
          .forceProperty("scala.version", "2.18.7")
          .addProfile("hadoop-foo")
          .addProfile("hadoop-bar")
          .addExclusion("org.scala-lang", "*")
          .addExclusion("org.scala-lang.modules", "*")
          .withUseSystemOsInfo(true)
          .withUseSystemJdkVersion(true)
          .withScalaVersion("2.14.3")
        val params0 = ApiHelper.resolutionParams(ApiHelper.resolutionParams(params))

        assert(params != ResolutionParams.create())
        assert(params == params0)
      }
    }

  }

}
