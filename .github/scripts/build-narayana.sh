#!/bin/bash
set -ex

if [ -z "$WORKSPACE" ]; then
  echo "WORKSPACE is unset"
  exit 1
fi

function fatal {
  echo "$1"
  exit 1
}

cd "$WORKSPACE"

if [ ! -d narayana-tmp ]; then
  NARAYANA_REPO=${NARAYANA_REPO:-jbosstm}
  NARAYANA_BRANCH="${NARAYANA_BRANCH:-main}"
  git clone "https://github.com/${NARAYANA_REPO}/narayana.git" -b "${NARAYANA_BRANCH}" narayana-tmp
  [ $? = 0 ] || fatal "git clone https://github.com/${NARAYANA_REPO}/narayana.git failed"
else
  NARAYANA_BRANCH="${NARAYANA_BRANCH:-main}"
  cd narayana-tmp
  git checkout "${NARAYANA_BRANCH}"
  git fetch origin
  git reset --hard "origin/${NARAYANA_BRANCH}"
  cd ../
fi

if [ -n "$NY_BRANCH" ]; then
  echo "Building Narayana PR branch: $NY_BRANCH"
  cd narayana-tmp
  git fetch origin +refs/pull/*/head:refs/remotes/jbosstm/pull/*/head
  [ $? = 0 ] || fatal "git fetch of pulls failed"
  git checkout "$NY_BRANCH"
  [ $? = 0 ] || fatal "git checkout of pull branch failed"
  cd ../
fi

./build.sh -f narayana-tmp/pom.xml clean install -B -DskipTests -Pcommunity
[ $? = 0 ] || fatal "Narayana build failed"

NARAYANA_VERSION=$(grep "<version>" narayana-tmp/pom.xml | head -n 2 | tail -n 1 | sed "s/ *<version>//" | sed "s#</version>##")
echo "Built Narayana version: $NARAYANA_VERSION"
