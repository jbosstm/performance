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

GIT_BRANCH=master THREAD_COUNTS="1 50 100 400" COMPARISON="com.arjuna.ats.jta.xa.performance.*StoreBenchmark.*" COMPARISON_COUNT=4 narayana/scripts/hudson/jenkins.sh
cp benchmark-output.txt benchmark-store-output.txt
cp benchmark.png benchmark-store.png

GIT_BRANCH=master THREAD_COUNTS="1 50 100 400" COMPARISON="io.narayana.perf.product.*Comparison.*" COMPARISON_COUNT=5 narayana/scripts/hudson/jenkins.sh
cp benchmark-output.txt benchmark-comparison-output.txt
cp benchmark.png benchmark-comparison.png

JMHARGS="-foe -i 1 -wi 4 -f 1 -t 240 -r 30" JVM_ARGS="-DMAX_ERRORS=10" ./narayana/scripts/hudson/benchmark.sh
cp benchmark-output.txt benchmark-benchmarksh-output.txt
cp benchmark.png benchmark-benchmarksh.png

./build.sh -f narayana/pom.xml clean package -DskipTests
wget https://ci.jboss.org/hudson/job/WildFly-latest-master/lastSuccessfulBuild/artifact/dist/target/wildfly-10.x.zip
rm -rf wildfly-dist
unzip wildfly-10.x.zip -d wildfly-dist
sed -i "s/8080/8180/g" wildfly-dist/*/standalone/configuration/standalone-full.xml
export JBOSS_HOME=$PWD/$(ls -d wildfly-dist/wildfly-*/ | head -n 1)
./build.sh -f comparison/pom.xml clean install -DskipTests
#  mvn -f comparison/rest-at/pom.xml test
#  mvn -f comparison/ws-at/pom.xml test
./build.sh -f comparison/jts/pom.xml test
./narayana/scripts/hudson/bm.sh

comment_on_pull "Pull passed: $BUILD_URL"
