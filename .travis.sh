#!/usr/bin/env bash
set -exo pipefail

TEST_VERSION="0.1.0-test"
sbt \
  +test \
  +evictionCheck \
  +compatibilityCheck \
  'set version in ThisBuild := "'"$TEST_VERSION"'"' \
  publishLocal 2>&1 | grep -v 'Maybe this is ' | grep -v '^Renamed '

# test that things work from JDK 11
# not actually building things from it, running into weird proguard issues…

# inspired by http://eed3si9n.com/all-your-jdks-on-travis-ci-using-jabba
TEST_JDK="openjdk@1.11.0-2"
export JABBA_HOME="$HOME/.jabba"
curl -sL https://raw.githubusercontent.com/shyiko/jabba/0.11.2/install.sh | bash
source "$HOME/.jabba/jabba.sh"
"$JABBA_HOME/bin/jabba" install "$TEST_JDK"
export JAVA_HOME="$JABBA_HOME/jdk/$TEST_JDK"
export PATH="$JAVA_HOME/bin:$PATH"
java -Xmx32m -version

export TEST_VERSION
sbt test
