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
  for product in $products; do
    for iteration in $iterations; do
      for thread in $threads; do
          for jts in $jtsModes; do
              echo mvn test -P $product -Diterations=$iteration -Dthreads=$thread -Djts=$jts
              mvn test -P $product -Diterations=$iteration -Dthreads=$thread -Djts=$jts -DobjectStoreDir=$storeDir
          done
        done
    done
  done
}
  
function set_run_options {

  #products="EAP6 EAP5 EAP6-JDKORB"
  products="EAP5 EAP6"
  iterations="1000 10000 100000"
  threads="1 10 100"
  jtsModes="true"
  [ -z $storeDir ] && storeDir="target/TxStoreDir"
}

# Allow the caller to abort the tests
trap 'bail_out' 1 2 3 15

# Install dependencies into the local repo
[ -d "$M2_REPO/org/jacorb/jacorb/4.6.1.GA" -a -d "$M2_REPO/org/jacorb/jacorb/2.3.1.patched" -a -d "$M2_REPO/logkit/LogKit/1.2" -a -d "$M2_REPO/org/apache/avalon/framework/avalon-framework/4.1.5" ] || ./scripts/install-EAP5-dependencies.sh

echo "sending output to target/results.txt"

set_run_options
run_tests

# if you want to do another set of runs with different options then set products, iterations etc
# and call run_tests again

cat target/results.txt

