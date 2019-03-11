package coursierapi

import coursier.internal.api.ApiHelper
import utest._

object DependencyTests extends TestSuite {

  val tests = Tests {

    'exclusions - {
      val dep = Dependency.of("org", "name", "1.2")
        .addExclusion("foo", "*")

      val dep0 = ApiHelper.dependency(ApiHelper.dependency(dep))

      assert(dep == dep0)
    }

  }

}
