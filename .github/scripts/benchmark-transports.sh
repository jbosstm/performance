#!/bin/bash
set -ex

if [ -z "$WORKSPACE" ]; then
  echo "WORKSPACE is unset"
  exit 1
fi

if [ -z "$OVERRIDE_NARAYANA_VERSION" ]; then
  echo "OVERRIDE_NARAYANA_VERSION is unset"
  exit 1
fi

function fatal {
  echo "$1"
  exit 1
}

function build_narayana_lra {
  cd "$WORKSPACE"
  export MAVEN_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=512m"

  LRA_REPO=${LRA_REPO:-jbosstm}
  LRA_BRANCH=${LRA_BRANCH:-main}
  rm -rf lra
  git clone "https://github.com/${LRA_REPO}/lra.git"
  [ $? = 0 ] || fatal "git clone https://github.com/${LRA_REPO}/lra.git failed"
  echo "Checking if need an LRA PR"
  echo "Building LRA PR ${LRA_BRANCH}"
  cd lra
  git fetch origin +refs/pull/*/head:refs/remotes/jbosstm/pull/*/head
  [ $? = 0 ] || fatal "git fetch of pulls failed"
  git checkout $LRA_BRANCH
  [ $? = 0 ] || fatal "Checkout failed: $BUILD_URL";

  ./build.sh clean install -B -DskipTests
  [ $? = 0 ] || fatal "Narayana LRA build failed"
  [ "$LRA_CURRENT_VERSION" ] || export LRA_CURRENT_VERSION=$(grep "<version>" pom.xml | head -n 2 | tail -n 1 | sed "s/ *<version>//" | sed "s#</version>##")

  cd ..
}

function init_jboss_home {
  [ -d "$JBOSS_HOME" ] || fatal "missing AS - $JBOSS_HOME is not a directory"
  echo "JBOSS_HOME=$JBOSS_HOME"
  cp "${JBOSS_HOME}/docs/examples/configs/standalone-xts.xml" "${JBOSS_HOME}/standalone/configuration"
  cp "${JBOSS_HOME}/docs/examples/configs/standalone-rts.xml" "${JBOSS_HOME}/standalone/configuration"
  CONF="${JBOSS_HOME}/bin/jboss-cli.xml"
  sed -e 's#^\(.*</jboss-cli>\)#<connection-timeout>30000</connection-timeout>\n\1#' "$CONF" > "$CONF.tmp" && mv "$CONF.tmp" "$CONF"
  grep 'connection-timeout' "${CONF}"
  echo 'JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=8797,server=y,suspend=n"' >> "$JBOSS_HOME/bin/standalone.conf"
}

function download_and_update_as {
  if [ -z "${WILDFLY_RELEASE_VERSION}" ]; then
    WILDFLY_RELEASE_VERSION=$(curl -sL https://api.github.com/repos/wildfly/wildfly/releases/latest | jq -r ".tag_name")
    [ $? -eq 0 ] || fatal "No WILDFLY_RELEASE_VERSION specified"
    echo "version=$WILDFLY_RELEASE_VERSION"
  fi

  cd "$WORKSPACE"

  filesToCheck=(~/.m2/repository/org/jboss/narayana/rts/restat-api/${OVERRIDE_NARAYANA_VERSION}/restat-api-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/rts/restat-bridge/${OVERRIDE_NARAYANA_VERSION}/restat-bridge-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/rts/restat-integration/${OVERRIDE_NARAYANA_VERSION}/restat-integration-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/rts/restat-util/${OVERRIDE_NARAYANA_VERSION}/restat-util-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/xts/jbossxts/${OVERRIDE_NARAYANA_VERSION}/jbossxts-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/jbosstxbridge/${OVERRIDE_NARAYANA_VERSION}/jbosstxbridge-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/jts/narayana-jts-integration/${OVERRIDE_NARAYANA_VERSION}/narayana-jts-integration-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/jts/narayana-jts-idlj/${OVERRIDE_NARAYANA_VERSION}/narayana-jts-idlj-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-service-base/${LRA_CURRENT_VERSION}/lra-service-base-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-service-base/${LRA_CURRENT_VERSION}/lra-service-base-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-service-base/${LRA_CURRENT_VERSION}/lra-service-base-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-coordinator-jar/${LRA_CURRENT_VERSION}/lra-coordinator-jar-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-client/${LRA_CURRENT_VERSION}/lra-client-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/narayana-lra/${LRA_CURRENT_VERSION}/narayana-lra-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-proxy-api/${LRA_CURRENT_VERSION}/lra-proxy-api-${LRA_CURRENT_VERSION}.jar)
  goOffline=false
  for fileToCheck in "${filesToCheck[@]}"; do
    echo "Checking $fileToCheck"
    [ -f "$fileToCheck" ] || goOffline=true
  done
  if [ $goOffline = true ]; then
      ./build.sh dependency:go-offline -DskipTests
  fi

  if [ ! -f "wildfly-${WILDFLY_RELEASE_VERSION}.zip" ]; then
    echo "Downloading AS"
    wget -N "https://github.com/wildfly/wildfly/releases/download/${WILDFLY_RELEASE_VERSION}/wildfly-${WILDFLY_RELEASE_VERSION}.zip"
    [ $? -eq 0 ] || fatal "Could not download https://github.com/wildfly/wildfly/releases/download/${WILDFLY_RELEASE_VERSION}/wildfly-${WILDFLY_RELEASE_VERSION}.zip"
  fi
  rm -rf "wildfly-${WILDFLY_RELEASE_VERSION}"
  unzip "wildfly-${WILDFLY_RELEASE_VERSION}.zip"
  cp ~/.m2/repository/org/jboss/narayana/rts/restat-api/${OVERRIDE_NARAYANA_VERSION}/restat-api-${OVERRIDE_NARAYANA_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/narayana/rts/main/restat-api-*.jar
  [ $? -eq 0 ] || fatal "Could not copy restat-api-${OVERRIDE_NARAYANA_VERSION}.jar"
  cp ~/.m2/repository/org/jboss/narayana/rts/restat-bridge/${OVERRIDE_NARAYANA_VERSION}/restat-bridge-${OVERRIDE_NARAYANA_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/narayana/rts/main/restat-bridge-*.jar
  [ $? -eq 0 ] || fatal "Could not copy restat-bridge-${OVERRIDE_NARAYANA_VERSION}.jar"
  cp ~/.m2/repository/org/jboss/narayana/rts/restat-integration/${OVERRIDE_NARAYANA_VERSION}/restat-integration-${OVERRIDE_NARAYANA_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/narayana/rts/main/restat-integration-*.jar
  [ $? -eq 0 ] || fatal "Could not copy restat-integration-${OVERRIDE_NARAYANA_VERSION}.jar"
  cp ~/.m2/repository/org/jboss/narayana/rts/restat-util/${OVERRIDE_NARAYANA_VERSION}/restat-util-${OVERRIDE_NARAYANA_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/narayana/rts/main/restat-util-*.jar
  [ $? -eq 0 ] || fatal "Could not copy restat-util-${OVERRIDE_NARAYANA_VERSION}.jar"
  cp ~/.m2/repository/org/jboss/narayana/xts/jbossxts/${OVERRIDE_NARAYANA_VERSION}/jbossxts-${OVERRIDE_NARAYANA_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/xts/main/jbossxts-*.jar
  [ $? -eq 0 ] || fatal "Could not copy jbossxts-${OVERRIDE_NARAYANA_VERSION}.jar"
  cp ~/.m2/repository/org/jboss/narayana/jbosstxbridge/${OVERRIDE_NARAYANA_VERSION}/jbosstxbridge-${OVERRIDE_NARAYANA_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/xts/main/jbosstxbridge-*.jar
  [ $? -eq 0 ] || fatal "Could not copy jbosstxbridge-${OVERRIDE_NARAYANA_VERSION}.jar"
  cp ~/.m2/repository/org/jboss/narayana/jts/narayana-jts-integration/${OVERRIDE_NARAYANA_VERSION}/narayana-jts-integration-${OVERRIDE_NARAYANA_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/jts/integration/main/narayana-jts-integration-*.jar
  [ $? -eq 0 ] || fatal "Could not copy narayana-jts-integration-${OVERRIDE_NARAYANA_VERSION}.jar"
  cp ~/.m2/repository/org/jboss/narayana/jts/narayana-jts-idlj/${OVERRIDE_NARAYANA_VERSION}/narayana-jts-idlj-${OVERRIDE_NARAYANA_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/jts/main/narayana-jts-idlj-*.jar
  [ $? -eq 0 ] || fatal "Could not copy narayana-jts-idlj-${OVERRIDE_NARAYANA_VERSION}.jar"
  cp ~/.m2/repository/org/jboss/narayana/lra/lra-service-base/${LRA_CURRENT_VERSION}/lra-service-base-${LRA_CURRENT_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/narayana/lra/lra-coordinator/main/lra-service-base-*.jar
  [ $? -eq 0 ] || fatal "Could not copy lra-service-base-${LRA_CURRENT_VERSION}.jar to lra-coordinator"
  cp ~/.m2/repository/org/jboss/narayana/lra/lra-service-base/${LRA_CURRENT_VERSION}/lra-service-base-${LRA_CURRENT_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/narayana/lra/lra-participant/main/lra-service-base-*.jar
  [ $? -eq 0 ] || fatal "Could not copy lra-service-base-${LRA_CURRENT_VERSION}.jar to lra-participant"
  cp ~/.m2/repository/org/jboss/narayana/lra/lra-coordinator-jar/${LRA_CURRENT_VERSION}/lra-coordinator-jar-${LRA_CURRENT_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/narayana/lra/lra-coordinator/main/lra-coordinator-jar-*.jar
  [ $? -eq 0 ] || fatal "Could not copy lra-coordinator.jar"
  cp ~/.m2/repository/org/jboss/narayana/lra/lra-client/${LRA_CURRENT_VERSION}/lra-client-${LRA_CURRENT_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/narayana/lra/lra-participant/main/lra-client-*.jar
  [ $? -eq 0 ] || fatal "Could not copy lra-client-${LRA_CURRENT_VERSION}.jar"
  cp ~/.m2/repository/org/jboss/narayana/lra/narayana-lra/${LRA_CURRENT_VERSION}/narayana-lra-${LRA_CURRENT_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/narayana/lra/lra-participant/main/narayana-lra-*.jar
  [ $? -eq 0 ] || fatal "Could not copy narayana-lra-${LRA_CURRENT_VERSION}.jar"
  cp ~/.m2/repository/org/jboss/narayana/lra/lra-proxy-api/${LRA_CURRENT_VERSION}/lra-proxy-api-${LRA_CURRENT_VERSION}.jar wildfly-${WILDFLY_RELEASE_VERSION}/modules/system/layers/base/org/jboss/narayana/lra/lra-participant/main/lra-proxy-api-*.jar
  [ $? -eq 0 ] || fatal "Could not copy lra-proxy-api-${LRA_CURRENT_VERSION}.jar"

  export JBOSS_HOME=${WORKSPACE}/wildfly-${WILDFLY_RELEASE_VERSION}

  init_jboss_home
  cd "$WORKSPACE"
}

MAVEN_OVERRIDE_NARAYANA_VERSION="-Dnarayana.version=${OVERRIDE_NARAYANA_VERSION}"

cd "$WORKSPACE"

./build.sh clean install -f narayana/pom.xml -DskipTests $MAVEN_OVERRIDE_NARAYANA_VERSION
[ $? = 0 ] || fatal "Narayana perf module build failed"

build_narayana_lra
download_and_update_as

cd "$WORKSPACE"
./build.sh -f comparison/pom.xml clean install -Parq
[ $? = 0 ] || fatal "Transport comparison failed"
