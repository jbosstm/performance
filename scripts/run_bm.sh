
# We could add -prof stack ?
# -i (1 iteration), -wi (10 warm ups), -r (300 seconds at each iteration)
# use java -jar <maven module>/target/benchmarks.jar -h for options
[ -z "${JMHARGS}" ] && JMHARGS="-t 240 -r 25 -f 2 -wi 5 -i 5"

[ -z "${WORKSPACE}" ] && WORKSPACE=`pwd`
MAVEN_HOME=$WORKSPACE/tools/maven
PATH=$MAVEN_HOME/bin:$PATH
ofile=$WORKSPACE/benchmark-output.txt

# run a set of benchmarks and copy the generated jmh csv files to $WORKSPACE
function run_bm {
  suffix=".\*"
  f=${2%$suffix}
  CSV_DIR="$1/target/jmh"
  [ -d $CSV_DIR ] || mkdir -p $CSV_DIR
  CSVF="$CSV_DIR/$f-$3.csv"

  java -Xms4096m -Xmx4096m -jar $1/target/benchmarks.jar "$2" $JMHARGS -rf csv -rff $CSVF

  cp $CSVF $WORKSPACE # the jmh plugin looks for csv files in $WORKSPACE
}

# build the product in order to calculate a baseline for the benchmark
function build_narayana_master {
  [[ -d tmp ]] || mkdir tmp
  cd tmp

  rm -rf narayana
  NARAYANA_REPO=${NARAYANA_REPO:-jbosstm}
  NARAYANA_BRANCH="${NARAYANA_BRANCH:-${GIT_BRANCH}}"
  git clone https://github.com/${NARAYANA_REPO}/narayana.git -b ${NARAYANA_BRANCH}
  cd narayana
  ./build.sh clean install -DskipTests
  cd ../../
}

BM1="com.hp.mwtests.ts.arjuna.performance.Performance1.*"
BM2="com.hp.mwtests.ts.arjuna.atomicaction.CheckedActionTest.*"
BM3="com.arjuna.ats.jta.xa.performance.JTAStoreTests.*"

function run_benchmarks {
  run_bm narayana/ArjunaCore/arjuna "$BM1" $1
  run_bm narayana/ArjunaJTA/jta "$BM3" $1
}

function regression_check {
  ls $WORKSPACE/*.csv > /dev/null 2>&1
  res=$?

  if [ $res = 0 ]; then
    # "$@" should only contain jvm args (such as -D -X etc)
    java "$@" -Xms4096m -Xmx4096m -cp narayana/tools/target/classes io.narayana.perf.jmh.Benchmark $WORKSPACE/*.csv >> $ofile
    res=$?
  else
    echo "no benchmarks to compare"
  fi

  return $res
}

if [ -v OVERRIDE_NARAYANA_VERSION ]; then
  MAVEN_OVERRIDE_NARAYANA_VERSION="-Dnarayana.version=${OVERRIDE_NARAYANA_VERSION}"
fi

function generate_csv_files {
  mvn -f narayana/pom.xml package -DskipTests $MAVEN_OVERRIDE_NARAYANA_VERSION # build the benchmarks uber jar
  run_benchmarks pr # run the benchmarks against the local maven repo (should be the PR)
  
  build_narayana_master # build narayana master
  mvn -f narayana/pom.xml package -DskipTests $MAVEN_OVERRIDE_NARAYANA_VERSION # build the benchmarks uber jar
  run_benchmarks master # run the benchmarks against this build of master
}

echo "JMH benchmark run (with args $JMHARGS)\n" > $ofile
generate_csv_files
regression_check "$@"
rv=$?

exit $rv
