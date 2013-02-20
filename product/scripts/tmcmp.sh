#!/bin/bash

echo "sending output to target/results.txt"

[ -d "$M2_REPO/org/jacorb/jacorb/4.6.1.GA" -a -d "$M2_REPO/logkit/LogKit/1.2" -a -d "$M2_REPO/org/apache/avalon/framework/avalon-framework/4.1.5" ] || ./scripts/install-EAP5-dependencies.sh

[ -f target/results.txt ] && cp target/results.txt target/results.txt.backup

date >> target/results.txt
#echo "unpatched jacorb" >> target/results.txt

# the total number of transactions per run should be 100 times more than the following
# but the throughput is currently so low that I've temporarily reduced the size.
objectStoreType=com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor
mvn test -P EAP6 -Diterations=2000 -Dthreads=10 -Djts=true -DobjectStoreType=$objectStoreType
#mvn test -P EAP6 -Diterations=200000 -Dthreads=100 -Djts=true -DobjectStoreType=$objectStoreType

mvn test -P EAP6 -Diterations=2000 -Dthreads=10 -Djts=true
mvn test -P EAP5 -Diterations=2000 -Dthreads=10 -Djts=true


mvn test -P EAP5 -Diterations=2000 -Dthreads=1 -Djts=true
mvn test -P EAP6 -Diterations=2000 -Dthreads=1 -Djts=true

#mvn test -P EAP5 -Diterations=20000 -Dthreads=10 -Djts=false
#mvn test -P EAP6 -Diterations=20000 -Dthreads=10 -Djts=false

#mvn test -P EAP5 -Diterations=200000 -Dthreads=1 -Djts=false
#mvn test -P EAP6 -Diterations=200000 -Dthreads=1 -Djts=false

cat target/results.txt
