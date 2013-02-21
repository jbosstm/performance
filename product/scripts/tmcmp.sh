#!/bin/bash

function bail_out {
  echo "Terminating tests"
  exit 1
}

trap 'bail_out' 1 2 3 15

function user_count {
    echo There are $(who | wc -l) users logged in \($(date)\)
}

echo "sending output to target/results.txt"

[ -d "$M2_REPO/org/jacorb/jacorb/4.6.1.GA" -a -d "$M2_REPO/logkit/LogKit/1.2" -a -d "$M2_REPO/org/apache/avalon/framework/avalon-framework/4.1.5" ] || ./scripts/install-EAP5-dependencies.sh

[ -f target/results.txt ] && cp target/results.txt target/results.txt.backup

# for proper testing you should multiply the number of iterations by 100 but on slow machines
# it would take a long time to complete
hqstore=com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor

mvn test -P EAP6 -Diterations=2000 -Dthreads=10 -Djts=true
mvn test -P EAP5 -Diterations=2000 -Dthreads=10 -Djts=true

mvn test -P EAP6 -Diterations=20000 -Dthreads=10 -Djts=true
mvn test -P EAP5 -Diterations=20000 -Dthreads=10 -Djts=true

mvn test -P EAP6 -Diterations=20000 -Dthreads=100 -Djts=true
mvn test -P EAP5 -Diterations=20000 -Dthreads=100 -Djts=true

mvn test -P EAP6 -Diterations=2000 -Dthreads=1 -Djts=true
mvn test -P EAP5 -Diterations=2000 -Dthreads=1 -Djts=true

mvn test -P EAP5 -Diterations=20000 -Dthreads=1 -Djts=true
mvn test -P EAP6 -Diterations=20000 -Dthreads=1 -Djts=true

mvn test -P EAP6 -Diterations=2000 -Dthreads=10 -Djts=true -DobjectStoreType=$hqstore
mvn test -P EAP6 -Diterations=20000 -Dthreads=100 -Djts=true -DobjectStoreType=$hqstore
#mvn test -P EAP6 -Diterations=200000 -Dthreads=100 -Djts=true -DobjectStoreType=$hqstore

#mvn test -P EAP6-JDKORB -Diterations=2000 -Dthreads=10 -Djts=true -DobjectStoreType=$hqstore
#mvn test -P EAP6-JDKORB -Diterations=200000 -Dthreads=100 -Djts=true -DobjectStoreType=$hqstore

#mvn test -P EAP6-JDKORB -Diterations=2000 -Dthreads=10 -Djts=true
#mvn test -P EAP6-JDKORB -Diterations=20000 -Dthreads=10 -Djts=true
#mvn test -P EAP6-JDKORB -Diterations=20000 -Dthreads=100 -Djts=true
#mvn test -P EAP6-JDKORB -Diterations=2000 -Dthreads=1 -Djts=true
#mvn test -P EAP6-JDKORB -Diterations=20000 -Dthreads=1 -Djts=true

cat target/results.txt
