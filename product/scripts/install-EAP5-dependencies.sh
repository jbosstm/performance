#!/bin/bash

function fatal {
  echo $@
  exit 1
}

[ -d "$M2_REPO" ] || fatal "please set M2_REPO because the build needs to manually install an artifact"

echo "installing 4 dependencies to $M2_REPO"
 
mvn install:install-file  -Dfile=lib/jacorb.4.6.1.GA.jar\
                          -DgroupId=org.jacorb \
                          -DartifactId=jacorb \
                          -Dversion=4.6.1.GA \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO

mvn install:install-file  -Dfile=lib/jacorb.2.3.1.patched.jar\
                          -DgroupId=org.jacorb \
                          -DartifactId=jacorb \
                          -Dversion=2.3.1.patched \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO

mvn install:install-file  -Dfile=lib/avalon-framework-4.1.5.jar \
                          -DgroupId=org.apache.avalon.framework \
                          -DartifactId=avalon-framework \
                          -Dversion=4.1.5 \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO

mvn install:install-file  -Dfile=lib/avalon-framework-4.1.5.jar \
                          -DgroupId=org.apache.avalon.framework \
                          -DartifactId=avalon-framework \
                          -Dversion=4.1.5 \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO

mvn install:install-file  -Dfile=lib/logkit-1.2.jar \
                          -DgroupId=logkit \
                          -DartifactId=LogKit \
                          -Dversion=1.2 \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO

# guess the version
mvn install:install-file  -Dfile=lib/jboss-iiop.jar \
                          -DgroupId=org.jboss.jbossas \
                          -DartifactId=jboss-as-iiop \
                          -Dversion=3.2.3 \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO

# guess the version
mvn install:install-file  -Dfile=lib/jnpserver.jar \
                          -DgroupId=org.jboss.jbossas \
                          -DartifactId=jnpserver \
                          -Dversion=3.2.3 \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO

