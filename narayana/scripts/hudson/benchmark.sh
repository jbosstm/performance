
function fatal {
  echo "$1"
  exit 1
}

# you can pass JVM args via an env variable:
# export JVM_ARGS="-Djmh.stack.lines=2"
[ -z "${WORKSPACE}" ] && fatal "WORKSPACE env variable is not set"
BMDIR=$WORKSPACE/narayana
[ -z "${BMDIR}" ] && fatal "directory $BMDIR does not exist"

# -i (1 iteration), -wi (10 warm ups), -r (300 seconds at each iteration)
# use java -jar <maven module>/target/benchmarks.jar -h for options
# profilers: gc,stack,comp
# NB if you want to profile with JFR on the oracle jvm use -XX:+UnlockCommercialFeatures and -prof jfr if supported

[ -z "${JMHARGS}" ] && JMHARGS="-foe -i 1 -wi 10 -f 1 -t 1 -r 100 -prof stack"
CJVM_ARGS="$JVM_ARGS"

RESFILE=$WORKSPACE/benchmark-output.txt

echo "JMH Benchmarks Results" > $RESFILE

function jotm_init {
  if [ -f "$1/target/classes/jotm.properties" ]; then
    mkdir -p $1/target/jotm/conf
    cp $1/target/classes/jotm.properties $1/target/jotm/conf
  fi
}

# run a set of benchmarks and copy the generated jmh csv files to $WORKSPACE
function run_bm {
  suffix=".\*"
  f=${2%$suffix}
  CSV_DIR="$1/target/jmh"
  [ -d $CSV_DIR ] || mkdir -p $CSV_DIR
  CSVF="$CSV_DIR/$f.csv"

  JVM_ARGS="$CJVM_ARGS"

  if [[ $2 == *"Comparison"* ]]; then
    jotm_init $1
    mkdir -p $1/target/bitronix
    mkdir -p $1/target/narayana
    mkdir -p $1/target/geronimo
    mkdir -p $1/target/atomikos

    JVM_ARGS="$JVM_ARGS -DBUILD_DIR=$1/target -Dcom.atomikos.icatch.file=$1/target/classes/atomikos.properties -DObjectStoreEnvironmentBean.objectStoreDir=$1/target/narayana -Dbitronix.tm.journal.disk.logPart1Filename=$1/target/bitronix/btm1.tlog -Dbitronix.tm.journal.disk.logPart2Filename=$1/target/bitronix/btm2.tlog -Djotm.base=$1/target/jotm -Dhowl.log.FileDirectory=$1/target/jotm"
  fi

  echo "run_bm with $1 and $2 and jvm args $JVM_ARGS"
  java -classpath $1/target/classes $JVM_ARGS -jar $1/target/benchmarks.jar "$2" $JMHARGS -rf csv -rff $CSVF

  [ $? = 0 ] || fatal "benchmark failure"
  [ -f $CSVF ] || fatal "JMH runner failed (missing csv output)"

  echo "" >> $RESFILE
  echo "Module: $1" >> $RESFILE
  echo "Pattern: $2" >> $RESFILE
  echo "Run arguments: $JMHARGS" >> $RESFILE
  echo "Run output:" >> $RESFILE
  cat $CSVF >> $RESFILE

  # there should be $3 results in the csv file
  let tc=$(wc -l < $CSVF)
  let tc=tc-1 # subtract 1 to account for the header
  [ $tc = $3 ] || fatal "Some benchmark tests did not finish. Expected: $3 Actual: $tc ($1 and $2)"
  
  #rm $CSVF
}

# run a benchmark against the local maven repo
function run_benchmarks {
  [ -d $1 ] || fatal "module directory $1 not found"
  bmjar="$1/target/benchmarks.jar"
  [ -f $bmjar ] || fatal "benchmark jar $bmjar not found"
  run_bm "$1" "$2" "$3"
}

# define which benchmarks to run. The syntax is:
# <maven module directory> <jmh test pattern> <the expected number of benchmarks>
# the need for the third field is because JMH does not return error codes for benchmark failures
BM1="ArjunaCore/arjuna com.hp.mwtests.ts.arjuna.performance.Performance1.* 2"
#ArjunaCore/arjuna/tests/classes/com/hp/mwtests/ts/arjuna/performance/Performance1.java
BM2="ArjunaCore/arjuna com.hp.mwtests.ts.arjuna.atomicaction.CheckedActionTest.* 2"
#ArjunaCore/arjuna/tests/classes/com/hp/mwtests/ts/arjuna/atomicaction/CheckedActionTest.java
BM3="ArjunaJTA/jta com.arjuna.ats.jta.xa.performance.JTAStoreTests.* 1"
#ArjunaJTA/jta/tests/classes/com/arjuna/ats/jta/xa/performance/JTAStoreTests.java
BM4="ArjunaJTA/jta io.narayana.perf.product.*Comparison.* 5"
#ArjunaJTA/jta/tests/classes/io/narayana/perf/product/ProductComparison.java
BM5="ArjunaJTA/jta com.arjuna.ats.jta.xa.performance.*StoreBenchmark.* 4"
#ArjunaJTA/jta/tests/classes/com/arjuna/ats/jta/xa/performance/HQStoreBenchmark.java
#ArjunaJTA/jta/tests/classes/com/arjuna/ats/jta/xa/performance/VolatileStoreBenchmark.java
#ArjunaJTA/jta/tests/classes/com/arjuna/ats/jta/xa/performance/ShadowNoFileLockStoreBenchmark.java
#ArjunaJTA/jta/tests/classes/com/arjuna/ats/jta/xa/performance/JDBCStoreBenchmark.java
BM6="ArjunaJTA/jta org.jboss.narayana.rts.*TxnTest.* 3"
#ArjunaJTA/jta/tests/classes/org/jboss/narayana/rts/TxnTest.java
#ArjunaJTA/jta/tests/classes/org/jboss/narayana/rts/NoTxnTest.java
#ArjunaJTA/jta/tests/classes/org/jboss/narayana/rts/EmptyTxnTest.java

cd $BMDIR
case $# in
0)
   for  i in "$BM1" "$BM2" "$BM3" "$BM4" "$BM5"; do
     IFS=' ' read -a bms <<< "$i"
     mvn -f "${bms[0]}/pom.xml" clean install -DskipTests # build the benchmarks
     run_benchmarks "${bms[0]}" "${bms[1]}" "${bms[2]}"
   done;;
1) fatal "syntax: module-dir benchmark-pattern";;
*) run_benchmarks "$1" "$2" "$3";;
esac

rv=$?
cd $WORKSPACE

exit $rv
