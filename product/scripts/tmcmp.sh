#!/bin/bash

function bail_out {
  echo "Terminating tests"
  exit 1
}

function is_store_compatible {
  [ "$1" = "EAP5" -a "$2" = "$hqStore" ]
}

function run_tests {
  for product in $products; do
    for iteration in $iterations; do
      for thread in $threads; do
          for store in $stores; do
              for jts in $jtsModes; do
                  is_store_compatible $product $store
                  if [ $? != 0 ]; then
                    echo mvn test -P $product -Diterations=$iteration -Dthreads=$thread -Djts=$jts
                    if [ "$store" = "default" ]; then
                      mvn test -P $product -Diterations=$iteration -Dthreads=$thread -Djts=$jts \
                        -DobjectStoreDir=$storeDir
                    else
                      mvn test -P $product -Diterations=$iteration -Dthreads=$thread -Djts=$jts \
                        -DobjectStoreType=$store -DobjectStoreDir=$storeDir
                    fi
                  fi
              done
          done
        done
    done
  done
}

function set_run_options {
  [ -z "$products" ] && products="EAP5 EAP6"
  [ -z "$iterations" ] && iterations="1000 10000 100000"
  [ -z "$threads" ] && threads="1 10 100"
  [ -z "$jtsModes" ] && jtsModes="true"
  [ -z "$storeDir" ] && storeDir="target/TxStoreDir"
  [ -z "$stores" ] && stores="default"
}

# Allow the caller to abort the tests
trap 'bail_out' 1 2 3 15

echo "sending output to target/results.txt"

set_run_options
run_tests

# if you want to do another set of runs with different options then set products, iterations etc
# and call run_tests again

cat target/results.txt

