
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

[ -z "${JMHARGS}" ] && JMHARGS="-i 1 -wi 10 -f 1 -t 1 -r 100 -prof stack"

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

  if [[ $2 == *"ProductComparison"* ]]; then
    jotm_init $1
    JVM_ARGS="$JVM_ARGS -Djotm.base=$1/target/jotm"
  fi

  echo "run_bm with $1 and $2 and jvm args $JVM_ARGS"
  java $JVM_ARGS -jar $1/target/benchmarks.jar "$2" $JMHARGS -rf csv -rff $CSVF

  [ $? = 0 ] || fatal "benchmark failure"

  echo "" >> $RESFILE
  echo "Module: $1" >> $RESFILE
  echo "Pattern: $2" >> $RESFILE
  echo "Run arguments: $JMHARGS" >> $RESFILE
  echo "Run output:" >> $RESFILE
  cat $CSVF >> $RESFILE
  rm $CSVF
}

# run a benchmark against the local maven repo
function run_benchmarks {
  [ -d $1 ] || fatal "module directory $1 not found"
  bmjar="$1/target/benchmarks.jar"
  [ -f $bmjar ] || fatal "benchmark jar $bmjar not found"
  run_bm "$1" "$2"
}

BM1="ArjunaCore/arjuna com.hp.mwtests.ts.arjuna.performance.Performance1.*"
BM2="ArjunaCore/arjuna com.hp.mwtests.ts.arjuna.atomicaction.CheckedActionTest.*"
BM3="ArjunaJTA/jta com.arjuna.ats.jta.xa.performance.JTAStoreTests.*"
BM4="ArjunaJTA/jta io.narayana.perf.product.ProductComparison.*"
BM5="ArjunaJTA/jta com.arjuna.ats.jta.xa.performance.*StoreBenchmark.*"

cd $BMDIR
case $# in
0) #mvn clean package test; # build the benchmarks
   for  i in "$BM1" "$BM2" "$BM3" "$BM4" "$XM5"; do
     IFS=' ' read -a bms <<< "$i"
     run_benchmarks "${bms[0]}" "${bms[1]}"
   done;;
1) fatal "syntax: module-dir benchmark-pattern";;
*) run_benchmarks "$1" "$2";;
esac

rv=$?
cd $WORKSPACE

exit $rv
