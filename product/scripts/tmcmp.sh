#!/bin/bash

date >> target/results.txt
echo "unpatched jacorb" >> target/results.txt

mvn test -P EAP5 -Diterations=20000 -Dthreads=10 -Djts=true
mvn test -P EAP6 -Diterations=20000 -Dthreads=10 -Djts=true

mvn test -P EAP5 -Diterations=200000 -Dthreads=1 -Djts=true
mvn test -P EAP6 -Diterations=200000 -Dthreads=1 -Djts=true

#mvn test -P EAP5 -Diterations=20000 -Dthreads=10 -Djts=false
#mvn test -P EAP6 -Diterations=20000 -Dthreads=10 -Djts=false

#mvn test -P EAP5 -Diterations=200000 -Dthreads=1 -Djts=false
#mvn test -P EAP6 -Diterations=200000 -Dthreads=1 -Djts=false
