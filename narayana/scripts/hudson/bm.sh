# use this script to perform different runs using different tunable config parameters. It is useful for determining the best config.

function cmp_narayana {
  XARGS="-DHornetqJournalEnvironmentBean.maxIO=$2 -DHornetqJournalEnvironmentBean.bufferFlushesPerSecond=$3 -DHornetqJournalEnvironmentBean.asyncIO=$4"
  if [ -z "${JMHARGS}" ] ; then
   JMHARGS="-t $1 -r 30 -f 3 -wi 5 -i 5"
  fi
  JMHARGS="-t $1 ${JMHARGS/-t*-r/ -r}" ./narayana/scripts/hudson/benchmark.sh "ArjunaJTA/jta" "io.narayana.perf.product.NarayanaComparison.*" 1 "$XARGS" > $5
  tput=$(grep NarayanaComparison.test $5  | tail -1  | tr -s ' ' | cut -d ' ' -f 4)
  if [ $# = 6 ]; then
    echo "evaluating $6'<'$tput | bc -l"
    gt=$(echo $6'<'$tput | bc -l)
  else
    gt=""
  fi

  echo -e "${1}\t${2}\t${3}\t${4}\t${tput}\t${gt}" >> results.txt
}

function runs {
  echo -e "threads\tmaxIO\tbf/s\tAIO\ttput\t\tchange" > results.txt
  for threads in 1 24 240 1600; do
    for bf in 300 2000 4000 7812; do
      cmp_narayana $threads 500 $bf true r-${threads}-${bf}.txt
      cmp_narayana $threads 1 $bf false r.txt $tput
    done
  done
}

runs
