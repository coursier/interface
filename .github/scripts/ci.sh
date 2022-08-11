#!/usr/bin/env bash
set -exo pipefail

export COURSIER_JNI="force"

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
  echo "Running JDK 11 tests…"
  # test that things work from JDK 11
  # not actually building things from it, running into weird proguard issues…

  TEST_JDK="adopt:1.11.0-7"
  eval "$(cs java --jvm "$TEST_JDK" --env)"

  java -Xmx32m -version

  export TEST_VERSION
  sbt test
fi
