#!/bin/bash

echo "sending output to target/results.txt"

date >> target/results.txt
echo "unpatched jacorb" >> target/results.txt

# the total number of transactions per run should be 100 times more than the following
# but the throughput is currently so low that I've temporarily reduced the size.
mvn test -P EAP5 -Diterations=200 -Dthreads=10 -Djts=true
mvn test -P EAP6 -Diterations=200 -Dthreads=10 -Djts=true

mvn test -P EAP5 -Diterations=2000 -Dthreads=1 -Djts=true
mvn test -P EAP6 -Diterations=2000 -Dthreads=1 -Djts=true

#mvn test -P EAP5 -Diterations=20000 -Dthreads=10 -Djts=false
#mvn test -P EAP6 -Diterations=20000 -Dthreads=10 -Djts=false

#mvn test -P EAP5 -Diterations=200000 -Dthreads=1 -Djts=false
#mvn test -P EAP6 -Diterations=200000 -Dthreads=1 -Djts=false
