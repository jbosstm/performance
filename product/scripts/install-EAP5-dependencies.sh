#!/bin/bash

function fatal {
  echo $@
  exit 1
}

[ -d "$M2_REPO" ] || fatal "please set M2_REPO because the build needs to manually install an artifact"
 
mvn install:install-file  -Dfile=etc/jacorb.4.6.1.GA.jar\
                          -DgroupId=org.jacorb \
                          -DartifactId=jacorb \
                          -Dversion=4.6.1.GA \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO

mvn install:install-file  -Dfile=etc/avalon-framework-4.1.5.jar \
                          -DgroupId=org.apache.avalon.framework \
                          -DartifactId=avalon-framework \
                          -Dversion=4.1.5 \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO

mvn install:install-file  -Dfile=etc/logkit-1.2.jar \
                          -DgroupId=logkit \
                          -DartifactId=LogKit \
                          -Dversion=1.2 \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO

