#!/bin/sh

function fatal {
    echo "fatal: $1"
    if [ -f "$2" ]; then
      cat $2
    fi

    exit 1
}

function test {
    calls=$1
    threads=$2
    transactional=$3
    enlist=$4
    remote=$5

    prepareDelay=0
    verbose=0

    qs="count=$calls&verbose=$verbose&prepareDelay=$prepareDelay&enlist=$enlist&remote=$remote&transactional=$transactional"

    for (( i=1; i<=$threads; i++ )); do
        if [ $i = $threads ]; then 
           curl http://localhost:8080/perf-war/PerfTest -d "$qs" > "res$i" 2>/dev/null
           proc[$i]=0
        else
           curl http://localhost:8080/perf-war/PerfTest -d "$qs" > "res$i" 2>/dev/null &
           proc[$i]=$!
        fi
    done

    tot=0
    for (( i=1; i<=$threads; i++ )); do
        pid=${proc[$i]}
        [ $pid != 0 ] && wait $pid

        cat "res$i" |grep -l html > /dev/null
        [ $? = 1 ] || fatal "run $i $1 $2" "res$i"; # failed

         v=$(cat res$i)
         tot=`expr $tot + $v`
    done

#    throughput=`expr $tot / $threads`
    throughput=$tot
    printf "%11s %8s %8s %13s %6s %6s\n" $throughput $calls $threads $transactional $enlist $remote
#    echo $calls transactions $threads threads: throughput=$throughput

    return 0
}

printf "%11s %8s %13s %8s %6s %6s\n" "Throughput" "Calls" "Threads" "Transactional" "Enlist" "Remote"

case $# in
0)
   test 1000 1 0 0 0
   test 1000 1 0 0 1
   echo ""
   test 10000 1 1 1 1
   test 1000 10 1 1 1
   echo ""
   test 100 10 1 0 1
   test 100 10 1 0 0
   test 100 10 1 2 1
   test 100 10 1 1 0
   ;;
1) test $1 3 1 1 1;;
2) test $1 $2 1 1 1;;
3) test $1 $2 $3 1 1;;
4) test $1 $2 $3 $4 1;;
*) test $1 $2 $3 $4 1;;
esac

#curl http://localhost:8080/perf-war/PerfTest -d "count=10&verbose=0" > /dev/null 2>&1
exit 0
