#!/bin/bash
set -x

if [ -z $WORKSPACE ]
then
    echo WORKSPACE is unset
    exit -1
fi

function fatal {
  comment_on_pull "Tests failed ($BUILD_URL): $1"
  echo "$1"
  exit 1
}

function comment_on_pull
{
    if [ "$COMMENT_ON_PULL" = "" ]; then return; fi

    PULL_NUMBER=$(echo $GIT_BRANCH | awk -F 'pull' '{ print $2 }' | awk -F '/' '{ print $2 }')
    if [ "$PULL_NUMBER" != "" ]
    then
        JSON="{ \"body\": \"$1\" }"
        curl -d "$JSON" -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/repos/$GIT_ACCOUNT/$GIT_REPO/issues/$PULL_NUMBER/comments
    else
        echo "Not a pull request, so not commenting"
    fi
}

# Keep ./narayana/scripts/hudson/jenkins.sh ./scripts/hudson/jenkins.sh ./scripts/run_bm.sh uniform
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
          comment_on_pull "Narayana build failed: $BUILD_URL";
          exit -1
      fi
      OVERRIDE_NARAYANA_VERSION=`grep "<version>" narayana-tmp/pom.xml | head -n 2 | tail -n 1 | sed "s/ *<version>//" | sed "s#</version>##"`
    fi
}


export GIT_ACCOUNT=jbosstm
export GIT_REPO=performance
export GIT_BRANCH="${GIT_BRANCH:-main}"

PULL_NUMBER=$(echo $GIT_BRANCH | awk -F 'pull' '{ print $2 }' | awk -F '/' '{ print $2 }')
PULL_DESCRIPTION=$(curl -H "Authorization: token $GITHUB_TOKEN" -s https://api.github.com/repos/$GIT_ACCOUNT/$GIT_REPO/pulls/$PULL_NUMBER)
if [[ $PULL_DESCRIPTION =~ "\"state\": \"closed\"" ]]; then
  echo "pull closed"
  exit 0
fi

PATH=$WORKSPACE/tmp/tools/maven/bin/:$PATH


comment_on_pull "Started testing this pull request: $BUILD_URL"

if [ ! -z "$JMHARGS" ]; then
  comment_on_pull "JMHARGS was overriden to: $JMHARGS"
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
./build.sh clean install -f narayana
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
	if [[ -z $APP_SERVER_ZIP_LOCATION ]]; then
		echo "Error, APP_SERVER_ZIP_LOCATION was not set"
		exit -1
	fi
	wget -q $APP_SERVER_ZIP_LOCATION
	[ $? = 0 ] || fatal "Could not download zip"
	unzip -q target.zip
	[ $? = 0 ] || fatal "Could not extract zip"
	cd target
	export WILDFLY_DIST_ZIP=$(ls wildfly-*-SNAPSHOT.zip)
	[ $? = 0 ] || fatal "Could not find WFLY"
	unzip -q $WILDFLY_DIST_ZIP
	[ $? = 0 ] || fatal "Could not extract WFLY"
	export WILDFLY_HOME=`pwd`/${WILDFLY_DIST_ZIP%.zip}
	export JBOSS_HOME="${WILDFLY_HOME}"
	[ ! -d "${JBOSS_HOME}" ] && fatal "JBOSS_HOME directory '${JBOSS_HOME}' does not exist"
	cd -
	./build.sh -f comparison/pom.xml clean install
	[ $? = 0 ] || fatal "Transport comparison failed"
fi

if [ -z $COMPARE_JOURNAL_PARAMETERS ] || [ $COMPARE_JOURNAL_PARAMETERS == "y" ]; then
	./narayana/scripts/hudson/bm.sh
	[ $? = 0 ] || fatal "BM properties failed"
fi

comment_on_pull "Pull passed: $BUILD_URL"
