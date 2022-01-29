# use this script to perform different runs using different tunable config parameters. It is useful for determining the best config.

function cmp_narayana {
  XARGS="-DHornetqJournalEnvironmentBean.maxIO=$2 -DHornetqJournalEnvironmentBean.bufferFlushesPerSecond=$3 -DHornetqJournalEnvironmentBean.asyncIO=$4"
  if [ -z "${JMHARGS}" ] ; then
   JMHARGS="-t $1 -r 30 -f 3 -wi 5 -i 5"
  fi
  echo "Running JMHARGS="-t $1 ${JMHARGS/-t*-r/ -r}" ./narayana/scripts/hudson/benchmark.sh "ArjunaJTA/jta" "io.narayana.perf.product.NarayanaComparison.*" 1 "$XARGS" > $5"
  JMHARGS="-t $1 ${JMHARGS/-t*-r/ -r}" ./narayana/scripts/hudson/benchmark.sh "ArjunaJTA/jta" "io.narayana.perf.product.NarayanaComparison.*" 1 "$XARGS" > $5
  field=4
  if grep "Measurement: 1 iterations" $5; then
    field=3
  fi
  tput=$(grep NarayanaComparison.test $5  | tail -1  | tr -s ' ' | cut -d ' ' -f $field)
  if [ $# = 6 ]; then
    echo "evaluating $6'<'$tput | bc -l"
    gt=$(echo $6'<'$tput | bc -l)
  else
    gt=""
  fi

  echo -e "${1}\t${2}\t${3}\t${4}\t${tput}\t${gt}" >> results.txt
}

function runs {

  if [ -z "${THREAD_COUNTS}" ]
  then
    THREAD_COUNTS="1 24 240 1600"
  fi

  if [ -z "${BF_COUNTS}" ]
  then
    BF_COUNTS="300 2000 4000 7812"
  fi

  echo -e "threads\tmaxIO\tbf/s\tAIO\ttput\t\tchange" > results.txt
  for threads in $THREAD_COUNTS; do
    for bf in $BF_COUNTS; do
      cmp_narayana $threads 500 $bf true r-${threads}-${bf}.txt
      cmp_narayana $threads 1 $bf false r.txt $tput
    done
  done
}

runs
