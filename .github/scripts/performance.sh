#!/bin/bash
set -x

if [ -z $WORKSPACE ]
then
  echo WORKSPACE is unset
  exit -1
fi

function fatal {
  echo "$1"
  exit 1
}

function build_narayana {
  if [ -z $BUILD_NARAYANA ] || [ $BUILD_NARAYANA != "n" ];
    then
    if [ ! -d narayana-tmp ]; then
      NARAYANA_REPO=${NARAYANA_REPO:-jbosstm}
      NARAYANA_BRANCH="${NARAYANA_BRANCH:-main}"
      git clone https://github.com/${NARAYANA_REPO}/narayana.git -b ${NARAYANA_BRANCH} narayana-tmp
      [ $? = 0 ] || fatal "git clone https://github.com/${NARAYANA_REPO}/narayana.git failed"
    else
      NARAYANA_BRANCH="${NARAYANA_BRANCH:-main}"
      cd narayana-tmp
      git checkout ${NARAYANA_BRANCH}
      git fetch origin
      git reset --hard origin/${NARAYANA_BRANCH}
      cd ../
    fi
    echo "Checking if need Narayana PR"
    if [ -n "$NY_BRANCH" ]; then
      echo "Building NY PR"
      cd narayana-tmp
      git fetch origin +refs/pull/*/head:refs/remotes/jbosstm/pull/*/head
      [ $? = 0 ] || fatal "git fetch of pulls failed"
      git checkout $NY_BRANCH
      [ $? = 0 ] || fatal "git fetch of pull branch failed"
      cd ../
    fi
    ./build.sh -f narayana-tmp/pom.xml clean install -B -DskipTests -Pcommunity
    if [ $? != 0 ]; then
        fatal "Narayana build failed"
    fi
    OVERRIDE_NARAYANA_VERSION=`grep "<version>" narayana-tmp/pom.xml | head -n 2 | tail -n 1 | sed "s/ *<version>//" | sed "s#</version>##"`
  fi
}

function build_narayana_lra {
  cd $WORKSPACE
  # INITIALIZE LRA ENV
  export MAVEN_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=512m"

  LRA_REPO=${LRA_REPO:-jbosstm}
  LRA_BRANCH=main
  #rm -rf ~/.m2/repository/
  rm -rf lra
  git clone https://github.com/${LRA_REPO}/lra.git -b ${LRA_BRANCH}
  [ $? = 0 ] || fatal "git clone https://github.com/${LRA_REPO}/lra.git failed"
  echo "Checking if need Narayana LRA PR"
  if [ -n "$LRA_PR_BRANCH" ]; then
    echo "Building LRA PR ${LRA_PR_BRANCH}"
    cd lra
    git fetch origin +refs/pull/*/head:refs/remotes/jbosstm/pull/*/head
    [ $? = 0 ] || fatal "git fetch of pulls failed"
    git checkout $LRA_PR_BRANCH
    [ $? = 0 ] || fatal "git fetch of pull branch failed"
    cd ../
  fi

  if [ $? != 0 ]; then
    echo "Checkout failed"
    exit -1
  fi
  cd lra
  ./build.sh clean install -B -DskipTests
  [ $LRA_CURRENT_VERSION ] || export LRA_CURRENT_VERSION=`grep "<version>" pom.xml | head -n 2 | tail -n 1 | sed "s/ *<version>//" | sed "s#</version>##"`
  cd ..

  if [ $? != 0 ]; then
    echo "Narayana LRA build failed";
    exit -1
  fi
}

function download_and_update_as {
  if [ -z "${WILDFLY_RELEASE_VERSION}" ]; then
    WILDFLY_RELEASE_VERSION=$(curl -sL https://api.github.com/repos/wildfly/wildfly/releases/latest | jq -r ".tag_name")
    [ $? -eq 0 ] ||fatal "No WILDFLY_RELEASE_VERSION specified"
    echo "version=$WILDFLY_RELEASE_VERSION"
  fi

  cd $WORKSPACE

  # Check if the needed files are available in the m2 cache
  filesToCheck=(~/.m2/repository/org/jboss/narayana/rts/restat-api/${OVERRIDE_NARAYANA_VERSION}/restat-api-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/rts/restat-bridge/${OVERRIDE_NARAYANA_VERSION}/restat-bridge-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/rts/restat-integration/${OVERRIDE_NARAYANA_VERSION}/restat-integration-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/rts/restat-util/${OVERRIDE_NARAYANA_VERSION}/restat-util-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/xts/jbossxts/${OVERRIDE_NARAYANA_VERSION}/jbossxts-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/jbosstxbridge/${OVERRIDE_NARAYANA_VERSION}/jbosstxbridge-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/jts/narayana-jts-integration/${OVERRIDE_NARAYANA_VERSION}/narayana-jts-integration-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/jts/narayana-jts-idlj/${OVERRIDE_NARAYANA_VERSION}/narayana-jts-idlj-${OVERRIDE_NARAYANA_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-service-base/${LRA_CURRENT_VERSION}/lra-service-base-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-service-base/${LRA_CURRENT_VERSION}/lra-service-base-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-service-base/${LRA_CURRENT_VERSION}/lra-service-base-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-coordinator-jar/${LRA_CURRENT_VERSION}/lra-coordinator-jar-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-client/${LRA_CURRENT_VERSION}/lra-client-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/narayana-lra/${LRA_CURRENT_VERSION}/narayana-lra-${LRA_CURRENT_VERSION}.jar ~/.m2/repository/org/jboss/narayana/lra/lra-proxy-api/${LRA_CURRENT_VERSION}/lra-proxy-api-${LRA_CURRENT_VERSION}.jar)
  goOffline=false
  for fileToCheck in "${filesToCheck[@]}"; do
    echo Checking $fileToCheck
    [ -f $fileToCheck ] || goOffline=true
  done
  if [ $goOffline = true ]; then
      ./build.sh dependency:go-offline -DskipTests
  fi

  if [ ! -f wildfly-${WILDFLY_RELEASE_VERSION}.zip ]; then
    echo "Downloading AS"
    wget -N  https://github.com/wildfly/wildfly/releases/download/${WILDFLY_RELEASE_VERSION}/wildfly-${WILDFLY_RELEASE_VERSION}.zip
    [ $? -eq 0 ] || fatal "Could not download https://github.com/wildfly/wildfly/releases/download/${WILDFLY_RELEASE_VERSION}/wildfly-${WILDFLY_RELEASE_VERSION}.zip"
  fi
  rm -rf wildfly-${WILDFLY_RELEASE_VERSION}
  unzip wildfly-${WILDFLY_RELEASE_VERSION}.zip
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
  cd $WORKSPACE
}

function init_jboss_home {
  [ -d $JBOSS_HOME ] || fatal "missing AS - $JBOSS_HOME is not a directory"
  echo "JBOSS_HOME=$JBOSS_HOME"
  cp ${JBOSS_HOME}/docs/examples/configs/standalone-xts.xml ${JBOSS_HOME}/standalone/configuration
  cp ${JBOSS_HOME}/docs/examples/configs/standalone-rts.xml ${JBOSS_HOME}/standalone/configuration
  # configuring bigger connection timeout for jboss cli (WFLY-13385)
  CONF="${JBOSS_HOME}/bin/jboss-cli.xml"
  sed -e 's#^\(.*</jboss-cli>\)#<connection-timeout>30000</connection-timeout>\n\1#' "$CONF" > "$CONF.tmp" && mv "$CONF.tmp" "$CONF"
  grep 'connection-timeout' "${CONF}"
  #Enable remote debugger
  echo JAVA_OPTS='"$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=8797,server=y,suspend=n"' >> "$JBOSS_HOME"/bin/standalone.conf
}

PATH=$WORKSPACE/tmp/tools/maven/bin/:$PATH

if [ ! -z "$JMHARGS" ]; then
  echo "JMHARGS was overriden to: $JMHARGS"
fi

cd $WORKSPACE
build_narayana

if [ -z "$THREAD_COUNTS" ]; then
   THREAD_COUNTS="1 24 240 1600"
fi

if [ -z $COMPARE_STORES ] || [ $COMPARE_STORES == "y" ]; then
	THREAD_COUNTS=$THREAD_COUNTS COMPARISON="com.arjuna.ats.jta.xa.performance.*StoreBenchmark.*" COMPARISON_COUNT=4 BM_LINE_PATTERN="(com.arjuna.ats.jta.xa.performance|i.n.p.p)" PATTERN2="Benchmark" narayana/scripts/hudson/jenkins.sh
	[ $? = 0 ] || fatal "Store benchmark failed"
	mv benchmark-output.txt benchmark-store-output.txt
	mv benchmark.png benchmark-store.png
fi

if [ -z $COMPARE_IMPLEMENTATIONS ] || [ $COMPARE_IMPLEMENTATIONS == "y" ]; then
	THREAD_COUNTS=$THREAD_COUNTS COMPARISON="io.narayana.perf.product.*Comparison.*" COMPARISON_COUNT=4 narayana/scripts/hudson/jenkins.sh
	[ $? = 0 ] || fatal "Product comparison benchmark failed"
	mv benchmark-output.txt benchmark-comparison-output.txt
	mv benchmark.png benchmark-comparison.png
fi

cd $WORKSPACE
./build.sh clean install
JVM_ARGS="-DMAX_ERRORS=10" ./narayana/scripts/hudson/benchmark.sh "ArjunaJTA/jta" "org.jboss.narayana.rts.*TxnTest.*" 3
[ $? = 0 ] || fatal "RTS benchmark failed"
mv benchmark-output.txt benchmark-rts-output.txt
mv benchmark.png benchmark-rts.png

if [ -v OVERRIDE_NARAYANA_VERSION ]; then
  MAVEN_OVERRIDE_NARAYANA_VERSION="-Dnarayana.version=${OVERRIDE_NARAYANA_VERSION}"
fi

if [ -z $COMPARE_TRANSPORTS ] || [ $COMPARE_TRANSPORTS == "y" ] || [ -z $COMPARE_JOURNAL_PARAMETERS ] || [ $COMPARE_JOURNAL_PARAMETERS == "y" ]; then
	./build.sh clean install -f narayana/pom.xml -DskipTests $MAVEN_OVERRIDE_NARAYANA_VERSION
fi

if [ -z $COMPARE_TRANSPORTS ] || [ $COMPARE_TRANSPORTS == "y" ]; then
  build_narayana_lra
  download_and_update_as
	cd -
	./build.sh -f comparison/pom.xml clean install -Parq
	[ $? = 0 ] || fatal "Transport comparison failed"
fi

if [ -z $COMPARE_JOURNAL_PARAMETERS ] || [ $COMPARE_JOURNAL_PARAMETERS == "y" ]; then
	./narayana/scripts/hudson/bm.sh
	[ $? = 0 ] || fatal "BM properties failed"
fi
