#!/bin/bash

objectStoreType=com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor

[ -f target/results.txt ] && cp target/results.txt target/results.txt.backup

date >> target/results.txt
mvn test -P EAP6 -Diterations=2000 -Dthreads=10 -Djts=true -DobjectStoreType=$objectStoreType

