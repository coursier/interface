# coursier-interface

*Zero-dependency Java API for coursier*

[![CI](https://github.com/coursier/interface/actions/workflows/ci.yml/badge.svg)](https://github.com/coursier/interface/actions/workflows/ci.yml)
[![Join the chat at https://gitter.im/coursier/coursier](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/coursier/coursier?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://img.shields.io/maven-central/v/io.get-coursier/interface.svg)](https://maven-badges.herokuapp.com/maven-central/io.get-coursier/interface)
[![javadoc](https://javadoc.io/badge2/io.get-coursier/interface/javadoc.svg)](https://javadoc.io/doc/io.get-coursier/interface)

*coursier-interface* is a zero-dependency Java library, exposing some of the features of the [API of coursier](https://get-coursier.io/docs/api). *coursier-interface* shades coursier, along with all its dependencies, so that it doesn't have any public dependency, and can be safely used along with other Scala or coursier versions.

*coursier-interface* aims at maintaining backward binary compatibility as much as possible. This means that if you depend on version N of coursier-interface, any version M >= N is safe to use at runtime. Backward binary compatibility has not been broken since the very first release of coursier-interface, `0.0.1` (ignoring version `0.0.11`, which exposed some dependencies that should have been shaded).

*coursier-interface* doesn't support as many features as the API of coursier itself. For now, it has equivalents for:
- `coursier.Fetch`: `coursierapi.Fetch`,
- `coursier.Versions`: `coursierapi.Versions`.

Note that all parameters of the inputs and results of these coursier APIs don't necessarily have equivalents in coursier-interface yet.

Beware that unlike their coursier counterparts, which are immutable, some coursier-interface classes may rely on mutation.

Missing features are added when they are needed. PRs adding missing features are welcome, as long as backward binary compatibility is not broken. See [development](#development) for more details about how to proceed.

## Development

This project is built with [sbt](https://www.scala-sbt.org). If sbt is not installed on your machine, you can easily get a launcher by using [sbt-extras](https://github.com/paulp/sbt-extras). Alternatively, if a recent version of [the coursier CLI](https://get-coursier.io/docs/cli-overview) is installed on your machine, you can either do `cs install sbt`, or start sbt straightaway from the root of the coursier-interface sources with `cs launch sbt`.

Most of the public APIs live under `interface/src/main/java/coursierapi`. New classes can be added there, and new constructors, fields, and methods, can be added to existing classes, as long as this doesn't break backward binary compatibility.

When these public classes need to call methods from coursier itself, they usually do so via [`coursier.internal.api.ApiHelper`](https://github.com/coursier/interface/blob/0b92aff32468c677b4dadc5af028a55a588aa9e5/interface/src/main/scala/coursier/internal/api/ApiHelper.scala). `ApiHelper` contains:
- methods converting coursier-interface public classes to their equivalent in the coursier API, and
- methods accepting classes of the public API, calling the coursier API itself (to perform some action, like fetching artifacts), and converting their results back to the public API classes.

Overall, the logic in the classes of the public API itself is kept at a minimum: setters / accessors, and calls to `coursier.internal.api.ApiHelper`.

## Code of Conduct

The coursier project welcomes contributions from anybody wishing to participate.
All code or documentation that is provided must be licensed with the same
license that coursier is licensed with (Apache 2.0, see LICENSE).

People are expected to follow the [Scala Code of Conduct](https://www.scala-lang.org/conduct)
when discussing coursier on GitHub, Gitter channel, or other venues.

Feel free to open an issue if you notice a bug, have an idea for a feature, or have a question about the code. Pull requests are also gladly accepted.
