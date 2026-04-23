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

PATH=$WORKSPACE/tmp/tools/maven/bin/:$PATH

if [ ! -z "$JMHARGS" ]; then
  echo "JMHARGS was overriden to: $JMHARGS"
fi

cd $WORKSPACE

if [ -z $BUILD_NARAYANA ] || [ $BUILD_NARAYANA != "n" ]; then
  WORKSPACE="$WORKSPACE" ./.github/scripts/build-narayana.sh
  [ $? = 0 ] || fatal "Narayana build failed"
  export OVERRIDE_NARAYANA_VERSION=$(grep "<version>" narayana-tmp/pom.xml | head -n 2 | tail -n 1 | sed "s/ *<version>//" | sed "s#</version>##")
fi

if [ -z "$THREAD_COUNTS" ]; then
   THREAD_COUNTS="1 24 240 1600"
fi

if [ -z $COMPARE_STORES ] || [ $COMPARE_STORES == "y" ]; then
	BUILD_NARAYANA=n THREAD_COUNTS=$THREAD_COUNTS COMPARISON="com.arjuna.ats.jta.xa.performance.*StoreBenchmark.*" COMPARISON_COUNT=4 BM_LINE_PATTERN="(com.arjuna.ats.jta.xa.performance|i.n.p.p)" PATTERN2="Benchmark" ./.github/scripts/benchmark-report.sh
	[ $? = 0 ] || fatal "Store benchmark failed"
	mv benchmark-output.txt benchmark-store-output.txt
	mv benchmark.png benchmark-store.png
fi

if [ -z $COMPARE_IMPLEMENTATIONS ] || [ $COMPARE_IMPLEMENTATIONS == "y" ]; then
	BUILD_NARAYANA=n THREAD_COUNTS=$THREAD_COUNTS COMPARISON="io.narayana.perf.product.*Comparison.*" COMPARISON_COUNT=4 ./.github/scripts/benchmark-report.sh
	[ $? = 0 ] || fatal "Product comparison benchmark failed"
	mv benchmark-output.txt benchmark-comparison-output.txt
	mv benchmark.png benchmark-comparison.png
fi

cd $WORKSPACE
./build.sh clean install
JVM_ARGS="-DMAX_ERRORS=10" ./.github/scripts/benchmark.sh "ArjunaJTA/jta" "org.jboss.narayana.rts.*TxnTest.*" 3
[ $? = 0 ] || fatal "RTS benchmark failed"
mv benchmark-output.txt benchmark-rts-output.txt
mv benchmark.png benchmark-rts.png

if [ -v OVERRIDE_NARAYANA_VERSION ]; then
  MAVEN_OVERRIDE_NARAYANA_VERSION="-Dnarayana.version=${OVERRIDE_NARAYANA_VERSION}"
fi

if [ -z $COMPARE_TRANSPORTS ] || [ $COMPARE_TRANSPORTS == "y" ]; then
  WORKSPACE="$WORKSPACE" OVERRIDE_NARAYANA_VERSION="$OVERRIDE_NARAYANA_VERSION" ./.github/scripts/benchmark-transports.sh
  [ $? = 0 ] || fatal "Transport comparison failed"
fi

if [ -z $COMPARE_JOURNAL_PARAMETERS ] || [ $COMPARE_JOURNAL_PARAMETERS == "y" ]; then
	./build.sh clean install -f narayana/pom.xml -DskipTests $MAVEN_OVERRIDE_NARAYANA_VERSION
	./.github/scripts/bm.sh
	[ $? = 0 ] || fatal "BM properties failed"
fi

