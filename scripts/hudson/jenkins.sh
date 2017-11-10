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
