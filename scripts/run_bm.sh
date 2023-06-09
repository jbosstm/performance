
# We could add -prof stack ?
# -i (1 iteration), -wi (10 warm ups), -r (300 seconds at each iteration)
# use java -jar <maven module>/target/benchmarks.jar -h for options
[ ! -z "${THREAD_COUNTS}" ] && THREAD_ARG=`echo $THREAD_COUNTS | cut -f 1 -d " "` || THREAD_ARG="240"
[ -z "${JMHARGS}" ] && JMHARGS="-t $THREAD_ARG -r 30 -f 3 -wi 5 -i 5"

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

# Keep ./narayana/scripts/hudson/jenkins.sh ./scripts/hudson/jenkins.sh ./scripts/run_bm.sh uniform
function build_narayana {
    if [ -z $BUILD_NARAYANA ] || [ $BUILD_NARAYANA != "n" ];
      then
      if [ ! -d narayana-tmp ]; then
        NARAYANA_REPO=${NARAYANA_REPO:-jbosstm}
        NARAYANA_BRANCH="${NARAYANA_BRANCH:-${GIT_BRANCH}}"
        git clone https://github.com/${NARAYANA_REPO}/narayana.git -b ${NARAYANA_BRANCH} narayana-tmp
        [ $? = 0 ] || fatal "git clone https://github.com/${NARAYANA_REPO}/narayana.git failed"
      else
        NARAYANA_BRANCH="${NARAYANA_BRANCH:-${GIT_BRANCH}}"
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
      ${WORKSPACE}/build.sh -f narayana-tmp/pom.xml clean install -B -DskipTests -Pcommunity
      if [ $? != 0 ]; then
          comment_on_pull "Narayana build failed: $BUILD_URL";
          exit -1
      fi
      OVERRIDE_NARAYANA_VERSION=`grep "<version>" narayana-tmp/pom.xml | head -n 2 | tail -n 1 | sed "s/ *<version>//" | sed "s#</version>##"`
    fi
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
  ${WORKSPACE}/build.sh -f narayana/pom.xml clean install -DskipTests $MAVEN_OVERRIDE_NARAYANA_VERSION # build the benchmarks uber jar
  run_benchmarks pr # run the benchmarks against the local maven repo (should be the PR)
  
  build_narayana

  ${WORKSPACE}/build.sh -f narayana/pom.xml clean install -DskipTests # build the benchmarks uber jar
  run_benchmarks main # run the benchmarks against this build of main
}

echo "JMH benchmark run (with args $JMHARGS)\n" > $ofile
generate_csv_files
regression_check "$@"
rv=$?

exit $rv
