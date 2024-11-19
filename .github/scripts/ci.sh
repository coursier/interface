#!/usr/bin/env bash
set -exo pipefail

# Re-enable when switching to an sbt version that pulls an lm-coursier version
# that has https://github.com/coursier/sbt-coursier/pull/403.
# export COURSIER_JNI="force"

TEST_VERSION="0.1.0-test"

echo "Running tests"
sbt +test

IS_UNIX="false"
case "$(uname -s)" in
  Linux*)     IS_UNIX="true";;
  Darwin*)    IS_UNIX="true";;
  *)
esac

if [ "$IS_UNIX" == "true" ]; then
  echo "Running MiMA checks"
  sbt +mimaReportBinaryIssues

  echo "Publishing locally"
  sbt \
    'set version in ThisBuild := "'"$TEST_VERSION"'"' \
    publishLocal
  echo "Running JDK 8 tests…"

  TEST_JDK="adoptium:8"
  eval "$(cs java --jvm "$TEST_JDK" --env)"

  java -Xmx32m -version

  export TEST_VERSION
  sbt test
fi
