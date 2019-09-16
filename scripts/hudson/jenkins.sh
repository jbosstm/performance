#!/bin/bash
set -x

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
        curl -d "$JSON" -ujbosstm-bot:$BOT_PASSWORD https://api.github.com/repos/$GIT_ACCOUNT/$GIT_REPO/issues/$PULL_NUMBER/comments
    else
        echo "Not a pull request, so not commenting"
    fi
}


export GIT_ACCOUNT=jbosstm
export GIT_REPO=performance

PULL_NUMBER=$(echo $GIT_BRANCH | awk -F 'pull' '{ print $2 }' | awk -F '/' '{ print $2 }')
PULL_DESCRIPTION=$(curl -ujbosstm-bot:$BOT_PASSWORD -s https://api.github.com/repos/$GIT_ACCOUNT/$GIT_REPO/pulls/$PULL_NUMBER)
if [[ $PULL_DESCRIPTION =~ "\"state\": \"closed\"" ]]; then
  echo "pull closed"
  exit 0
fi

PATH=$WORKSPACE/tmp/tools/maven/bin/:$PATH

comment_on_pull "Started testing this pull request: $BUILD_URL"

GIT_BRANCH=master THREAD_COUNTS="1 24 240 1600" COMPARISON="com.arjuna.ats.jta.xa.performance.*StoreBenchmark.*" COMPARISON_COUNT=4 narayana/scripts/hudson/jenkins.sh
[ $? = 0 ] || fatal "Store benchmark failed"
mv benchmark-output.txt benchmark-store-output.txt
mv benchmark.png benchmark-store.png

GIT_BRANCH=master THREAD_COUNTS="1 24 240 1600" COMPARISON="io.narayana.perf.product.*Comparison.*" COMPARISON_COUNT=5 narayana/scripts/hudson/jenkins.sh
[ $? = 0 ] || fatal "Product comparison benchmark failed"
mv benchmark-output.txt benchmark-comparison-output.txt
mv benchmark.png benchmark-comparison.png

# JBTM-3193 temporary disable (also, it might be the case that 3 tests can be ran when this is reenabled)
#JVM_ARGS="-DMAX_ERRORS=10" ./narayana/scripts/hudson/benchmark.sh "ArjunaJTA/jta" "org.jboss.narayana.rts.*TxnTest.*" 2
#[ $? = 0 ] || fatal "RTS benchmark failed"
#mv benchmark-output.txt benchmark-rts-output.txt
#mv benchmark.png benchmark-rts.png

./build.sh -f narayana/pom.xml clean package -DskipTests
wget -q http://narayanaci1.eng.hst.ams2.redhat.com/job/narayana-AS800/lastSuccessfulBuild/artifact/dist/target/*zip*/target.zip
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
./narayana/scripts/hudson/bm.sh
[ $? = 0 ] || fatal "BM properties failed"

comment_on_pull "Pull passed: $BUILD_URL"
