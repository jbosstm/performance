#!/bin/bash

host=`hostname`

function bail_out {
  echo "Terminating tests"
  exit 1
}

# check whether a give object store is available with a given version of the product
function is_store_compatible {
  hqStore="com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor"
  [ "$1" = "EAP5" -a "$2" = "$hqStore" ]
}

# run a test configuration - if simulating then print what would have been tested
function mvn_test {
  let testrun++
  if [ ! -z "$simulate" ]; then
    printf "%4s %8s %8s %10d %10d %7d %7d %6d %5b %8s %5s %8d %8b %8d\n" \
      "host" "04:10:34" $product -1 $iteration $warmUp $thread -1 $jts $xstore $aio $bufferSize $syncDelete $flushRate
  else
    echo "TEST: $testrun of $testcount: mvn test -P $product -Diterations=$iteration -Dthreads=$thread -Djts=$jts -DwarmUpIterations=$warmUp -DobjectStoreDir=$storeDir $@"
    # run the actual test
    mvn test -P $product -Diterations=$iteration -DwarmUpIterations=$warmUp -Dthreads=$thread -Djts=$jts -DobjectStoreDir=$storeDir $@
    res=$(cat target/surefire-reports/com.arjuna.ats.tools.perftest.task.ProductPerformanceTest-output.txt |grep $host)
    echo "TEST RESULT $testrun of $testcount: $res"
  fi
}

# test various configurations of the transaction manager (defaulting to use the file based object store)
function run_fstests {
  if [ ! -z "$simulate" ]; then aio="-"; bufferSize=-1; syncDelete=false; flushRate=-1; fi

  for product in $products; do
    for iteration in $iterations; do
      for thread in $threads; do
          for store in $stores; do
              for jts in $jtsModes; do
                  is_store_compatible $product $store
                  if [ $? != 0 ]; then
                    if [ "$store" = "default" ]; then
                      [ -z "$simulate" ] || xstore=default
                      mvn_test
                    else
                      [ -z "$simulate" ] || xstore=other
                      mvn_test -DobjectStoreType=$store
                    fi
                  fi
              done
          done
        done
    done
  done
}

# test the hornetq object store
function hqtest {
  product=$8
  xopts="$xargs -Djts=$6"
  [ -z "$simulate" ] || xstore=Hornetq

  if [ $7 = "aio" ]; then
   mvn_test \
   -Dcom.arjuna.ats.arjuna.hornetqjournal.bufferFlushesPerSecond=$3 \
   -Dcom.arjuna.ats.arjuna.hornetqjournal.bufferSize=$4 \
   -Dcom.arjuna.ats.arjuna.hornetqjournal.syncDeletes=$5 \
   -DobjectStoreType=com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor \
   -Daio=true \
   $xopts
  else
   mvn_test \
   -Dcom.arjuna.ats.arjuna.hornetqjournal.bufferFlushesPerSecond=$3 \
   -Dcom.arjuna.ats.arjuna.hornetqjournal.bufferSize=$4 \
   -Dcom.arjuna.ats.arjuna.hornetqjournal.syncDeletes=$5 \
   -DobjectStoreType=com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor \
   -Daio=false \
   -Daio.lib.path= \
   $xopts
  fi
}

# test various configurations of the hornetq object store
function run_hqtests {
  for product in $products; do
    [ $product = "EAP5" ] && continue
    for aio in $aioModes; do
      for jts in $jtsModes; do
        for syncDelete in $syncDeletes; do
          for iteration in $iterations; do
            for thread in $threads; do
              for bufferSize in $bufferSizes; do
                for flushRate in $flushRates; do
                  hqtest $iteration $thread $flushRate $bufferSize $syncDelete $jts $aio $product
                done
              done
            done
          done
        done
      done
    done
  done
}

# configure what will be tested
function set_run_options {
# to override any option set it as an env variable, for example:
# export iterations=100 bufferSizes=501760 flushRates="150 300 storeDir=/mnt/EAP6/data0"
#-Dcom.arjuna.ats.arjuna.hornetqjournal.maxIO=1 # default is 1 for NIO
#-Dcom.arjuna.ats.arjuna.hornetqjournal.maxIO=500 # default is 500 for NIO
#-Dcom.arjuna.ats.arjuna.hornetqjournal.syncDeletes=false # false slows down recovery

  # if the caller hasn't set an option via an env variable then set it to a default values:
  [ -z "$products" ] && products="EAP5 EAP6"
  [ -z "$stores" ] && stores="default"

  [ -z "$iterations" ] && iterations="10000"
  [ -z "$warmUp" ] && warmUp="0"
  [ -z "$threads" ] && threads="10"
  [ -z "$jtsModes" ] && jtsModes="true"
  [ -z "$storeDir" ] && storeDir="target/TxStoreDir"

  [ -z "$bufferSizes" ] && bufferSizes="501760 1003520" # default is 490 * 1024
  [ -z "$flushRates" ] && flushRates="150 300 600" # default=500
  [ -z "$syncDeletes" ] && syncDeletes="true false"
  [ -z "$maxIO" ] || xargs="$xargs -Dcom.arjuna.ats.arjuna.hornetqjournal.maxIO=$maxIO"
  [ -z "$aioModes" ] && aioModes="aio nio" # NB maybe validate that aioModes is aio or nio
}

function run_tests {
  # first simulate the tests to indicate what test configurations will be executed
  sim=$simulate
  simulate=true
  testrun=0

  echo "Will run the following test configurations:"
  printf "%4s %8s %8s %10s %10s %10s %7s %6s %5s %8s %5s %8s %8s %8s\n" "Host" "Time" "Product" "Throughput" "Iterations" "WarmUp" "Threads" "Aborts" "JTS" "Store" "AIO" "HQBuffSz" "SyncDel" "SyncRate"

  [ -z $skipFSTest ] && run_fstests # default is to run against the default object store
  [ -z $testhq ] || run_hqtests	# only test the hornetq store if explicitly requested

  if [ -z $sim ]; then
    # now run the test confurations for real
    unset simulate
    let testcount=testrun
    let testrun=0
 
    [ -z $skipFSTest ] && run_fstests
    [ -z $testhq ] || run_hqtests

    date >> target/tmresults.txt
    [ -f target/results.txt ] && cat target/results.txt >> target/tmresults.txt
    rm -f target/results.txt
  else
    echo "Simulation only - no real tests executed"
  fi
}

# Allow the caller to abort the tests
trap 'bail_out' 1 2 3 15

set_run_options
run_tests

