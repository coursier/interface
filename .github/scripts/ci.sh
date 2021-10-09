#!/usr/bin/env bash
set -exo pipefail

TEST_VERSION="0.1.0-test"
sbt \
  +test \
  +evictionCheck \
  +mimaReportBinaryIssues \
  'set version in ThisBuild := "'"$TEST_VERSION"'"' \
  publishLocal

# test that things work from JDK 11
# not actually building things from it, running into weird proguard issues…

TEST_JDK="adopt:1.11.0-7"
eval "$(cs java --jvm "$TEST_JDK" --env)"

java -Xmx32m -version

export TEST_VERSION
sbt test
