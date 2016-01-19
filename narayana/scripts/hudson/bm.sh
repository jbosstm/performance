# use this script to perform different runs using different tunable config parameters. It is useful for determining the best config.

PERIOD=50
WARMUP=4

function cmp_narayana {
  XARGS="-DHornetqJournalEnvironmentBean.maxIO=$2 -DHornetqJournalEnvironmentBean.bufferFlushesPerSecond=$3 -DHornetqJournalEnvironmentBean.asyncIO=$4"
  JMHARGS="-foe -i 1 -wi $WARMUP -f 1 -t $1 -r $PERIOD" ./narayana/scripts/hudson/benchmark.sh "ArjunaJTA/jta" "io.narayana.perf.product.NarayanaComparison.*" 1 "$XARGS" > $5
  tput=$(tail -1 $5 | tr -s ' ' | cut -d ' ' -f 4)
  if [ $# = 6 ]; then
    echo "evaluating $6'<'$tput | bc -l"
    gt=$(echo $6'<'$tput | bc -l)
  else
    gt=""
  fi

  echo -e "${1}\t${2}\t${3}\t${4}\t${tput}\t${gt}" >> results
}

function cmp_all {
  XARGS="-DHornetqJournalEnvironmentBean.maxIO=$2 -DHornetqJournalEnvironmentBean.bufferFlushesPerSecond=$3 -DHornetqJournalEnvironmentBean.asyncIO=$4"
  JMHARGS="-foe -i 1 -wi $WARMUP -f 1 -t $1 -r $PERIOD" ./narayana/scripts/hudson/benchmark.sh "ArjunaJTA/jta" "io.narayana.perf.product.*Comparison.*" 5 "$XARGS" > $5
  atps=$(tail -5 $5 | head -1 | tr -s ' ' | cut -d ' ' -f 4)
  btps=$(tail -4 $5 | head -1 | tr -s ' ' | cut -d ' ' -f 4)
  gtps=$(tail -3 $5 | head -1 | tr -s ' ' | cut -d ' ' -f 4)
  jtps=$(tail -2 $5 | head -1 | tr -s ' ' | cut -d ' ' -f 4)
  ntps=$(tail -1 $5 | head -1 | tr -s ' ' | cut -d ' ' -f 4)

  echo -e "${1}\t${2}\t${3}\t${4}\t${atps}\t${btps}\t${gtps}\t${jtps}\t${ntps}" >> results
}

function runs {
  echo -e "threads\tmaxIO\tbf/s\tAIO\ttput\t\tchange" > results
  for threads in 1 100 300 400; do
    for maxio in 1 10 100 300 500; do
      for bf in 100 1000 2000 3000 4000; do
        cmp_narayana $threads $maxio $bf true r.txt
        cmp_narayana $threads $maxio $bf false r.txt $tput
      done
    done
  done
}

function runs2 {
  echo -e "threads\tmaxIO\tbf/s\tAIO\ttput\t\tchange" > results
  #for threads in 1 100 300 400; do
  for threads in 100; do
    for maxio in 500; do
      for bf in 2000 ; do # 
        cmp_narayana $threads $maxio $bf true r.txt
#        cmp_narayana $threads $maxio $bf false r.txt $tput
      done
    done
  done
}

function runs3 {
  echo -e "threads\tmaxIO\tbf/s\tAIO\tatomikos\tbitronix\tgeronimo\tjotm\tnarayana" > results
  for threads in 1 100 300 400; do
    for maxio in 500; do
      for bf in 2000 ; do
        cmp_all $threads $maxio $bf true r.txt
      done
    done
  done
}


runs2
