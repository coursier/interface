package coursierapi

import coursier.{LocalRepositories, Repositories}
import coursier.internal.api.ApiHelper
import utest._

object RepositoryTests extends TestSuite {

  val tests = Tests {

    test("maven") {
      val initialRepo = MavenRepository.of("https://artifacts.corp.com")

      test("simple") {
        val repo = initialRepo
        val repo0 = ApiHelper.repository(ApiHelper.repository(repo))
        assert(repo == repo0)
      }

      test("credentials") {
        val repo = MavenRepository.of(initialRepo)
          .withCredentials(Credentials.of("a", "1234"))
        val repo0 = ApiHelper.repository(ApiHelper.repository(repo))
        assert(repo != initialRepo)
        assert(repo == repo0)
      }

      test("ivy2Local") {
        val toFromIvy2Local = ApiHelper.repository(Repository.ivy2Local())
        val ivy2Local = LocalRepositories.ivy2Local
        assert(ivy2Local == toFromIvy2Local)
      }

      test("central") {
        val toFromCentral = ApiHelper.repository(Repository.central())
        val central = Repositories.central
        assert(central == toFromCentral)
      }
    }

  }

}
