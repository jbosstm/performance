#!/bin/bash -e

# this script can be configured via environment variables. The best way
# to get a good understanding of how it works is to study the bash scripts launched
# by this script. Briefly, the following variables can be configured:

# NARAYANA_REPO: the git repo to use (which is useful for testing different repos
# NARAYANA_BRANCH: and the git branch to check out (which is useful for testing different branches)
# OVERRIDE_NARAYANA_VERSION: the version of narayana that should be benchmarked
# JMHARGS: arguments passed to the JMH, use `java -jar target/benchmarks.jar -h` for the available options
# THREAD_COUNTS: the number of threads to use for running benchmarks as comma separated list
#   for example THREAD_COUNTS="10 50 100" means perform 3 runs using 10 threads for the first run etc
# COMPARISON: a regex pattern which determines which java class names to use when looking for benchmarks
# COMPARISON_COUNT: and this is the expected number of class names that match the pattern

# build the main narayana repo
function build_narayana {
    if [ -z $BUILD_NARAYANA ] || [ $BUILD_NARAYANA != "n" ]; then
      WORKSPACE="$WORKSPACE" ./.github/scripts/build-narayana.sh
      if [ $? != 0 ]; then
          echo "Narayana build failed"
          exit 1
      fi
      OVERRIDE_NARAYANA_VERSION=$(grep "<version>" narayana-tmp/pom.xml | head -n 2 | tail -n 1 | sed "s/ *<version>//" | sed "s#</version>##")
    fi
}

# dump information about the OS enviroment and hardware (this is so that we determing the capacity of the platorm where the benchmarks run)
function preamble {
  export JVM_ARGS="-DMAX_ERRORS=10"
  os=`uname -a`
  ptype=`cat /proc/cpuinfo | grep "model name" | head -1`
  pcnt=`cat /proc/cpuinfo | grep processor | wc -l`
  date >> $1
  echo "Platform: $os" >> $1
  echo "Processor: $ptype" >> $1
  echo "Number of Cores: $pcnt" >> $1
  java -version >> $1
  echo -e "Blog Text\n=========" >> $1
}

# run all of the benchmarks in the ArjunaJTA/jta module
function bm {
  echo "NEXT RUN using $JMHARGS"
  chmod 755 ./.github/scripts/benchmark.sh
  ./.github/scripts/benchmark.sh "ArjunaJTA/jta" "$2" "$3"
  [ $? = 0 ] || res=1
  cat benchmark-output.txt >> $1
  echo "RUN status: $res"
  return $res
}

# encode a string according to the rules laid out in https://www.ietf.org/rfc/rfc2396.txt
function urlencode {
    local LANG=C
    local length="${#1}"
    for (( i = 0; i < length; i++ )); do
        local c="${1:i:1}"
        case $c in
            [a-zA-Z0-9.~_-]) printf "$c" ;;
            *) printf '%%%02X' "'$c" ;;
        esac
    done
}

build_narayana

res=0 

# the version of narayana that is being benchmarked
if [ -v OVERRIDE_NARAYANA_VERSION ]; then
  MAVEN_OVERRIDE_NARAYANA_VERSION="-Dnarayana.version=${OVERRIDE_NARAYANA_VERSION}"
fi

# build the narayana project
./build.sh -f narayana/pom.xml clean install -DskipTests $MAVEN_OVERRIDE_NARAYANA_VERSION

rm -f bm-output.txt benchmark-output.txt benchmark.png

if [ -z ${THREAD_COUNTS+x} ]
then
  THREAD_COUNTS="1 10 50 100 300 400"
fi

if [ -z ${COMPARISON+x} ] 
then
  COMPARISON="io.narayana.perf.product.*Comparison.*"
  COMPARISON_COUNT=4
fi

# run the benchmarks using various numbers of threads for the workload
for i in $THREAD_COUNTS
do
  if [ -z "${JMHARGS}" ] ; then
   JMHARGS="-t $i -r 30 -f 3 -wi 5 -i 5 -foe true "
  fi
  JMHARGS="-t $i ${JMHARGS/-t*-r/ -r}" bm bm-output.txt $COMPARISON $COMPARISON_COUNT
done

cp bm-output.txt benchmark-output.txt
preamble benchmark-output.txt

if [[ $(uname) == CYGWIN* ]]
then
  separator=";"
else
  separator=":"
fi

# use -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints to enable async profiling
java -Xms4096m -Xmx4096m -Danonymize=true -classpath "narayana/ArjunaJTA/jta/target/classes"$separator"narayana/ArjunaJTA/jta/target/benchmarks.jar" io.narayana.perf.product.ReportGenerator bm-output.txt $BM_LINE_PATTERN $PATTERN2 >> benchmark-output.txt
cat benchmark-output.txt


exit $res
