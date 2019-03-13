package coursierapi

import coursier.internal.api.ApiHelper
import utest._

object RepositoryTests extends TestSuite {

  val tests = Tests {

    'maven - {
      val initialRepo = MavenRepository.of("https://artifacts.corp.com")

      'simple {
        val repo = initialRepo
        val repo0 = ApiHelper.repository(ApiHelper.repository(repo))
        assert(repo == repo0)
      }

      'credentials {
        val repo = MavenRepository.of(initialRepo)
          .withCredentials(Credentials.of("a", "1234"))
        val repo0 = ApiHelper.repository(ApiHelper.repository(repo))
        assert(repo != initialRepo)
        assert(repo == repo0)
      }
    }

  }

}
