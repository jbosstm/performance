#!/bin/bash

function bail_out {
  echo "Terminating tests"
  exit 1
}

function user_count {
    echo There are $(who | wc -l) users logged in \($(date)\)
}

function is_store_compatible {
  [ "$1" = "EAP5" -a "$2" = "$hqStore" ]
}

function run_tests {
  for iteration in $iterations; do
    for thread in $threads; do
      for store in $stores; do
        for product in $products; do
          for jts in $jtsModes; do
            is_store_compatible $product $store
            if [ $? != 0 ]; then
              mvn test -P $product -Diterations=$iteration -Dthreads=$thread -Djts=$jts \
                -DobjectStoreType=$store -DobjectStoreDir=$storeDir
            fi
          done
        done
      done
    done
  done
}
  
function set_run_options {
  fileStore=com.arjuna.ats.internal.arjuna.objectstore.ShadowNoFileLockStore
  hqStore=com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor

  #products="EAP6 EAP5 EAP6-JDKORB"
  products="EAP6 EAP5"
  iterations="1000 10000 100000"
  threads="1 10 100"
  stores="$fileStore $hqStore"
  storeDir="target/TxStoreDir"
  jtsModes="true"
}

# Allow the caller to abort the tests
trap 'bail_out' 1 2 3 15

# Install dependencies into the local repo
[ -d "$M2_REPO/org/jacorb/jacorb/4.6.1.GA" -a -d "$M2_REPO/org/jacorb/jacorb/2.3.1.patched" -a -d "$M2_REPO/logkit/LogKit/1.2" -a -d "$M2_REPO/org/apache/avalon/framework/avalon-framework/4.1.5" ] || ./scripts/install-EAP5-dependencies.sh

echo "sending output to target/results.txt"
[ -f target/results.txt ] && cp target/results.txt target/results.txt.previous

set_run_options
run_tests

# if you want to do another set of runs with different options then set products, iterations etc
# and call run_tests again

cat target/results.txt

