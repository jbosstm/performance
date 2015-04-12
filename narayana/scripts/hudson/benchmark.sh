
function fatal {
  echo "$1"
  exit 1
}

#MAVEN_HOME=$WORKSPACE/tools/maven
#PATH=$MAVEN_HOME/bin:$PATH

[ -z "${WORKSPACE}" ] && fatal "WORKSPACE env variable is not set"
BMDIR=$WORKSPACE/narayana
[ -z "${BMDIR}" ] && fatal "directory $BMDIR does not exist"

# -i (1 iteration), -wi (10 warm ups), -r (300 seconds at each iteration)
# use java -jar <maven module>/target/benchmarks.jar -h for options
[ -z "${JMHARGS}" ] && JMHARGS="-i 1 -wi 10 -f 1 -t 1 -r 100"

RESFILE=$WORKSPACE/benchmark-output.txt

echo "JMH Benchmarks Results" > $RESFILE


# run a set of benchmarks and copy the generated jmh csv files to $WORKSPACE
function run_bm {
  suffix=".\*"
  f=${2%$suffix}
  CSV_DIR="$f/target/jmh"
  [ -d $CSV_DIR ] || mkdir -p $CSV_DIR
  CSVF="$CSV_DIR/$f-$3.csv"

  java -jar $1/target/benchmarks.jar "$2" $JMHARGS -rf csv -rff $CSVF

  echo "" >> $RESFILE
  echo "Module: $1" >> $RESFILE
  echo "Pattern: $2" >> $RESFILE
  echo "Run arguments: $JMHARGS" >> $RESFILE
  echo "Run output:" >> $RESFILE
  cat $CSVF >> $RESFILE
  rm $CSVF
}

# build the benchmarks
function build_benchmark {
  cd $BMDIR

  mvn clean test # build the benchmarks
}

BM1="com.hp.mwtests.ts.arjuna.performance.Performance1.*"
BM2="com.hp.mwtests.ts.arjuna.atomicaction.CheckedActionTest.*"
BM3="com.arjuna.ats.jta.xa.performance.JTAStoreTests.*"
BM4="io.narayana.perf.product.ProductComparison.*"
BM5="com.arjuna.ats.jta.xa.performance.*StoreBenchmark.*"

function run_benchmarks {
  cd $BMDIR
  run_bm $BMDIR/ArjunaCore/arjuna "$BM1" $1
  run_bm $BMDIR/ArjunaCore/arjuna "$BM2" $1
  run_bm $BMDIR/ArjunaJTA/jta "$BM3" $1
  run_bm $BMDIR/ArjunaJTA/jta "$BM4" $1
  run_bm $BMDIR/ArjunaJTA/jta "$BM5" $1
}

function generate_csv_files {
  build_benchmark # build the benchmarks

  run_benchmarks bm # run the benchmarks against the local maven repo
}

generate_csv_files
rv=$?
cd $WORKSPACE

exit $rv
