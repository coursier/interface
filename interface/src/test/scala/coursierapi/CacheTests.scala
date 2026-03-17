package coursierapi

import coursier.cache.{CacheEnv, FileCache}
import coursier.credentials.{DirectCredentials, FileCredentials}
import coursier.internal.api.ApiHelper
import coursier.util.EnvValues
import utest._

import java.nio.file.Files

object CacheTests extends TestSuite {

  val tests = Tests {

    test("credentials") {

      test("configFileCredentialsAutoLoaded") {
        val tmpFile = Files.createTempFile("test-credentials", ".properties")
        try {
          val content =
            """myrepo.host=artifacts.corp.com
              |myrepo.username=testuser
              |myrepo.password=testpass
              |""".stripMargin
          Files.write(tmpFile, content.getBytes)

          val credEnv = EnvValues(Some(tmpFile.toAbsolutePath.toString), None)
          val noEnv = EnvValues(None, None)
          val defaultCreds = CacheEnv.defaultCredentials(credEnv, noEnv, noEnv)

          val fc = FileCache().withCredentials(defaultCreds)
          val resolved = fc.credentials.flatMap {
            case dc: DirectCredentials => Seq(dc)
            case other => other.get()
          }

          assert(resolved.exists(_.host == "artifacts.corp.com"))
          assert(resolved.exists(dc => dc.usernameOpt.contains("testuser")))
          assert(resolved.exists(dc => dc.passwordOpt.exists(_.value == "testpass")))
        } finally {
          Files.deleteIfExists(tmpFile)
        }
      }

      test("defaultCredentialsNotOverridden") {
        val cache = Cache.create()
        val fc = ApiHelper.cache(cache)
        val defaultFc = FileCache()
        assert(fc.credentials == defaultFc.credentials)
      }

      test("explicitCredentialsAppendedToDefaults") {
        val cache = Cache.create()
          .addCredentials(Credentials.of("custom.host.com", "myuser", "mypass"))
        val fc = ApiHelper.cache(cache)
        val defaultFc = FileCache()
        assert(fc.credentials.size >= defaultFc.credentials.size + 1)
        val allDirect = fc.credentials.flatMap {
          case dc: DirectCredentials => Seq(dc)
          case other => other.get()
        }
        assert(allDirect.exists(_.host == "custom.host.com"))
      }

      test("multipleCredentialsFromPropertiesFile") {
        val tmpFile = Files.createTempFile("test-credentials-multi", ".properties")
        try {
          val content =
            """repo1.host=host1.example.com
              |repo1.username=user1
              |repo1.password=pass1
              |repo2.host=host2.example.com
              |repo2.username=user2
              |repo2.password=pass2
              |repo2.realm=MyRealm
              |repo2.https-only=true
              |""".stripMargin
          Files.write(tmpFile, content.getBytes)

          val credEnv = EnvValues(Some(tmpFile.toAbsolutePath.toString), None)
          val noEnv = EnvValues(None, None)
          val defaultCreds = CacheEnv.defaultCredentials(credEnv, noEnv, noEnv)

          val resolved = defaultCreds.flatMap {
            case dc: DirectCredentials => Seq(dc)
            case other => other.get()
          }

          assert(resolved.length == 2)
          val hosts = resolved.map(_.host).toSet
          assert(hosts == Set("host1.example.com", "host2.example.com"))

          val repo2 = resolved.find(_.host == "host2.example.com").get
          assert(repo2.realm.contains("MyRealm"))
          assert(repo2.httpsOnly)

          // Verify round-trip through Java API conversion
          val apiCred = ApiHelper.credentials(repo2)
          assert(apiCred.getHost == "host2.example.com")
          assert(apiCred.getRealm == "MyRealm")
          assert(apiCred.isHttpsOnly)

          val backToScala = ApiHelper.directCredentials(apiCred)
          assert(backToScala.host == "host2.example.com")
          assert(backToScala.realm.contains("MyRealm"))
          assert(backToScala.httpsOnly)
        } finally {
          Files.deleteIfExists(tmpFile)
        }
      }

      test("fetchAddCredentials") {
        val fetch = Fetch.create()
          .addCredentials(Credentials.of("fetch.host.com", "fuser", "fpass"))
        val creds = fetch.getCache.getCredentials
        assert(creds.size == 1)
        assert(creds.get(0).getHost == "fetch.host.com")
        assert(creds.get(0).getUser == "fuser")

        // Verify flows through to FileCache
        val fc = ApiHelper.cache(fetch.getCache)
        val allDirect = fc.credentials.flatMap {
          case dc: DirectCredentials => Seq(dc)
          case other => other.get()
        }
        assert(allDirect.exists(_.host == "fetch.host.com"))
      }

      test("completeAddCredentials") {
        val complete = Complete.create()
          .addCredentials(Credentials.of("complete.host.com", "cuser", "cpass"))
        val creds = complete.getCache.getCredentials
        assert(creds.size == 1)
        assert(creds.get(0).getHost == "complete.host.com")
      }

      test("versionsAddCredentials") {
        val versions = Versions.create()
          .addCredentials(Credentials.of("versions.host.com", "vuser", "vpass"))
        val creds = versions.getCache.getCredentials
        assert(creds.size == 1)
        assert(creds.get(0).getHost == "versions.host.com")
      }

      test("allPathsPreserveDefaults") {
        // Verify that Fetch, Complete, Versions all get default credentials
        // when no explicit credentials are set
        val defaultFc = FileCache()

        val fetchFc = ApiHelper.cache(Fetch.create().getCache)
        assert(fetchFc.credentials == defaultFc.credentials)

        val completeFc = ApiHelper.cache(Complete.create().getCache)
        assert(completeFc.credentials == defaultFc.credentials)

        val versionsFc = ApiHelper.cache(Versions.create().getCache)
        assert(versionsFc.credentials == defaultFc.credentials)
      }
    }
  }
}
