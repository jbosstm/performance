
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
  if [ -f "target/classes/jotm.properties" ]; then
    mkdir -p target/jotm/conf
    cp target/classes/jotm.properties target/jotm/conf
  fi
}

function mk_output_dirs {
  for d in jotm bitronix narayana geronimo atomikos; do
    mkdir -p target/$d
    rm -rf target/$d/*
  done
}

# run a set of benchmarks and copy the generated jmh csv files to $WORKSPACE
function run_bm {
  suffix=".\*"
  f=${2%$suffix}
  cd $1
  CSV_DIR="target/jmh"
  [ -d $CSV_DIR ] || mkdir -p $CSV_DIR
  CSVF="$CSV_DIR/$f.csv"

  JVM_ARGS="$CJVM_ARGS"

  if [[ $2 == *"Comparison"* ]]; then
    mk_output_dirs
    jotm_init

    JVM_ARGS="$JVM_ARGS $4 -DBUILD_DIR=target -Dcom.atomikos.icatch.file=target/classes/atomikos.properties -Dcom.atomikos.icatch.log_base_dir=target/atomikos -Dcom.arjuna.ats.arjuna.common.propertiesFile=jbossts-properties.xml -Dbitronix.tm.journal.disk.logPart1Filename=target/bitronix/btm1.tlog -Dbitronix.tm.journal.disk.logPart2Filename=target/bitronix/btm2.tlog -Djotm.base=target/jotm -Dhowl.log.FileDirectory=target/jotm"
  fi

  echo "java -classpath target/classes $JVM_ARGS -jar target/benchmarks.jar $2 $JMHARGS -rf csv -rff $CSVF"
  java -classpath target/classes $JVM_ARGS -jar target/benchmarks.jar "$2" $JMHARGS -rf csv -rff $CSVF
  res=$?

  if [ $res != 0 ]; then
    echo "benchmark run failed"
    return $res
  fi

  if [ ! -f $CSVF ]; then
    echo "JMH runner failed (missing csv output)"
    return 1
  fi

  echo "" >> $RESFILE
  echo "Module: $1" >> $RESFILE
  echo "Pattern: $2" >> $RESFILE
  echo "Run arguments: $JMHARGS" >> $RESFILE
  echo "Run output:" >> $RESFILE
  cat $CSVF >> $RESFILE

  # there should be $3 results in the csv file
  let tc=$(wc -l < $CSVF)
  let tc=tc-1 # subtract 1 to account for the header
  if [ $tc != $3 ]; then
    echo "Some benchmark tests did not finish. Expected: $3 Actual: $tc ($1 and $2)"
    return 1
  fi
}

# run a benchmark against the local maven repo
function run_benchmarks {
  [ -d $1 ] || fatal "module directory $1 not found"
  bmjar="$1/target/benchmarks.jar"
  [ -f $bmjar ] || fatal "benchmark jar $bmjar not found"
  run_bm "$1" "$2" "$3" "$4"
  return $?
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
BM4a="ArjunaJTA/jta io.narayana.perf.product.BitronixComparison.* 1"
BM4b="ArjunaJTA/jta io.narayana.perf.product.GeronimoComparison.* 1"
BM4c="ArjunaJTA/jta io.narayana.perf.product.NarayanaComparison.* 1"
BM4d="ArjunaJTA/jta io.narayana.perf.product.AtomikosComparison.* 1"
BM4e="ArjunaJTA/jta io.narayana.perf.product.JotmComparison.* 1"
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
res=0
case $# in
0)
   for  i in "$BM4a" "$BM4b" "$BM4c" "$BM4d" "$BM4e" "$BM1" "$BM2" "$BM3" "$BM5"; do
     IFS=' ' read -a bms <<< "$i"
     cd $BMDIR
     mvn -f "${bms[0]}/pom.xml" clean install -DskipTests # build the benchmarks
     run_benchmarks "${bms[0]}" "${bms[1]}" "${bms[2]}"
     [ $? = 0 ] || res=1
   done;;
1) fatal "syntax: module-dir benchmark-pattern";;
*) run_benchmarks "$1" "$2" "$3" "$4"
   res=$?;;
esac

cd $WORKSPACE

exit $res
