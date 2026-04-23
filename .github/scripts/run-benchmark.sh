#!/bin/bash
set -ex

BENCHMARK=$1

if [ -z "$BENCHMARK" ]; then
  echo "Usage: $0 <benchmark-name>"
  echo "  benchmark-name: stores | comparisons | rts | transports | journal"
  exit 1
fi

if [ -z "$WORKSPACE" ]; then
  echo "WORKSPACE is unset"
  exit 1
fi

MAVEN_OVERRIDE=""
if [ -n "$OVERRIDE_NARAYANA_VERSION" ]; then
  MAVEN_OVERRIDE="-Dnarayana.version=${OVERRIDE_NARAYANA_VERSION}"
fi

case "$BENCHMARK" in
  stores)
    BUILD_NARAYANA=n \
      WORKSPACE="$WORKSPACE" \
      THREAD_COUNTS="${THREAD_COUNTS:-1 24 240 1600}" \
      COMPARISON="com.arjuna.ats.jta.xa.performance.*StoreBenchmark.*" \
      COMPARISON_COUNT=4 \
      BM_LINE_PATTERN="(com.arjuna.ats.jta.xa.performance|i.n.p.p)" \
      PATTERN2="Benchmark" \
      ./.github/scripts/benchmark-report.sh
    mv benchmark-output.txt benchmark-store-output.txt
    mv benchmark.png benchmark-store.png
    ;;

  comparisons)
    BUILD_NARAYANA=n \
      WORKSPACE="$WORKSPACE" \
      THREAD_COUNTS="${THREAD_COUNTS:-1 24 240 1600}" \
      COMPARISON="io.narayana.perf.product.*Comparison.*" \
      COMPARISON_COUNT=4 \
      ./.github/scripts/benchmark-report.sh
    mv benchmark-output.txt benchmark-comparison-output.txt
    mv benchmark.png benchmark-comparison.png
    ;;

  rts)
    ./build.sh -f narayana/pom.xml clean install -DskipTests $MAVEN_OVERRIDE
    JVM_ARGS="-DMAX_ERRORS=10" WORKSPACE="$WORKSPACE" \
      ./.github/scripts/benchmark.sh "ArjunaJTA/jta" "org.jboss.narayana.rts.*TxnTest.*" 3
    mv benchmark-output.txt benchmark-rts-output.txt
    ;;

  transports)
    git config --global user.email "github-actions[bot]@users.noreply.github.com"
    git config --global user.name "github-actions[bot]"
    WORKSPACE="$WORKSPACE" ./.github/scripts/benchmark-transports.sh
    ;;

  journal)
    ./build.sh -f narayana/pom.xml clean install -DskipTests $MAVEN_OVERRIDE
    WORKSPACE="$WORKSPACE" ./.github/scripts/bm.sh
    ;;

  *)
    echo "Unknown benchmark: $BENCHMARK"
    exit 1
    ;;
esac
