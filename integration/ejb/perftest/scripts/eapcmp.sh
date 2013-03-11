#!/bin/bash

BASE_DIR=`pwd`
RES_DIR=$BASE_DIR/results/$$
RES_FILE=$RES_DIR/perf.$$.tab
PERF_REPO=https://github.com/jbosstm/performance
DR3_ZIP="http://download.devel.redhat.com/devel/candidates/JBEAP/JBEAP-6.1.0-DR3/jboss-eap-6.1.0.DR3.zip"
PROD_DIR=$BASE_DIR/integration/ejb/perftest
EAP5_DIR=$BASE_DIR/eap-5.1.1
EAP6_DIR=$BASE_DIR/eap-6.0
DR3_DIR=$BASE_DIR/dr3

EAP5_WAIT=60
EAP6_WAIT=20
store_type=FileStore
jacorb_patch=false
version="-"
versions="EAP6 EAP5"
tests=()
index=0

[ $EAP5_ZIP ] || EAP5_ZIP="http://download.devel.redhat.com/released/JBEAP-5/5.1.1/zip/jboss-eap-5.1.1.zip"
[ $EAP6_ZIP ] || EAP6_ZIP="http://download.devel.redhat.com/released/JBEAP-6/6.0.1/zip/jboss-eap-6.0.1.zip"
 
[ $JBOSS_TERM ] || JBOSS_TERM=gnome 

server0_pid=0
server1_pid=0

function bail_out {
  echo "Terminating tests"
  stop_servers
  exit 1
} 

function fatal {
    echo "fatal: $1" | tee -a $RES_FILE
    [ -f "$2" ] && cat $2

    bail_out
}

function do_copy {
  if [ -d $1 ]; then
    cp -r $1 $2
  else
   cp $1 $2
  fi

  [ $? = 0 ] || fatal "do_copy: $1 $2 failed"
}

function modify_jacorb_config {
  sed -i -e 's/3528/3628/g' $1
  sed -i -e 's/3529/3629/g' $1
# NB if we every want to recovery performance testing also change jacorb.implname
#  sed -i -e 's/jacorb.implname=JBoss/jacorb.implname=JBoss1/g' $1
}

function configure_eap5 {
  [ -d "$EAP5_DIR" ] || mkdir -p $EAP5_DIR

  zipfile=$BASE_DIR/jboss-eap-5.1.1.zip

  if [ ! -f $zipfile ]; then
    cd $BASE_DIR
    wget $EAP5_ZIP
    [ $? = 0 ] || fatal "configure_eap5: wget $EAP5_ZIP failed"
  fi

  if [ ! -d $EAP5_DIR/jboss-eap-5.1 ]; then
    unzip -d $EAP5_DIR $zipfile
    [ $? = 0 ] || fatal "configure_eap5: unzip EAP5 failed"

    cd $EAP5_DIR/jboss-eap-5.1/jboss-as/server
    echo "admin=admin" >> all/conf/props/jmx-console-users.properties
    do_copy all server0
    do_copy all server1
    cd ../docs/examples/transactions/
    ant jts -Dtarget.server.dir=../../../server/server0
    [ $? = 0 ] || fatal "enable EAP5 jts failed for server0"
    ant jts -Dtarget.server.dir=../../../server/server1
    [ $? = 0 ] || fatal "enable EAP5 jts failed for server1"
    modify_jacorb_config $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server1/conf/jacorb.properties

    do_copy  $EAP5_DIR/jboss-eap-5.1/jboss-as/server/all/lib/jacorb.jar $PROD_DIR/etc/jacorb.jar.eap5
  fi

  rm "$EAP5_DIR/jboss-eap-5.1/jboss-as/server/server0/log/*" > /dev/null 2>&1
  rm "$EAP5_DIR/jboss-eap-5.1/jboss-as/server/server1/log/*" > /dev/null 2>&1

}

function configure_eap6 {
  [ -d "$EAP6_DIR" ] || mkdir -p $EAP6_DIR

  zipfile=$BASE_DIR/jboss-eap-6.0.1.zip

  if [ ! -f $zipfile ]; then
    cd $BASE_DIR
    wget $EAP6_ZIP
    [ $? = 0 ] || fatal "configure_eap6: wget $EAP6_ZIP failed"
  fi

  if [ ! -d $EAP6_DIR/jboss-eap-6.0 ]; then
    unzip -d $EAP6_DIR $zipfile
    [ $? = 0 ] || fatal "configure_eap6: unzip EAP6 failed"

    cd $EAP6_DIR

    do_copy jboss-eap-6.0 server0
    do_copy jboss-eap-6.0 server1

    do_copy  $EAP6_DIR/jboss-eap-6.0/modules/org/jacorb/main/jacorb-2.3.2-redhat-2.jar $PROD_DIR/etc/jacorb.jar.eap6

    enable_jts EAP6 0
    enable_jts EAP6 1
    stop_servers

    do_copy  $EAP6_DIR/server0/standalone/configuration/standalone-full.xml $PROD_DIR/standalone-full-server0.xml
    do_copy  $EAP6_DIR/server1/standalone/configuration/standalone-full.xml $PROD_DIR/standalone-full-server1.xml

    enable_hornetq_os EAP6 0
    enable_hornetq_os EAP6 1
    stop_servers

    do_copy  $EAP6_DIR/server0/standalone/configuration/standalone-full.xml $PROD_DIR/standalone-full-hq-server0.xml
    do_copy  $EAP6_DIR/server1/standalone/configuration/standalone-full.xml $PROD_DIR/standalone-full-hq-server1.xml
  fi

  rm "$EAP6_DIR/server0/standalone/log/*" > /dev/null 2>&1
  rm "$EAP6_DIR/server1/standalone/log/*" > /dev/null 2>&1
}

function clone_perf_repo {
  if [ ! -d $BASE_DIR/.git ]; then
    cd $BASE_DIR
    git clone $PERF_REPO .
    [ $? = 0 ] || fatal "clone performance repo failed"
  fi
}

function update_ear {
  cd $PROD_DIR
  [ $? = 0 ] || fatal "perftest dir doesn't exist"

#  if [ ! -f perf-eap5/harness/ear/target/ear-1.0.ear ]; then
    mvn clean install -f perf-api/pom.xml
    mvn clean install -f perf-eap6/pom.xml
#    mvn clean install
    [ $? = 0 ] || fatal "perftest clean install failed"
#  fi

  do_copy perf-eap5/harness/ear/target/ear-1.0.ear $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server0/deploy/
  do_copy perf-eap5/harness/ear2/target/ear2-1.0.ear $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server1/deploy/

  do_copy perf-eap6/application-component-1/target/perf-eap6-app-component-1.war $EAP6_DIR/server0/standalone/deployments/
  do_copy perf-eap6/application-component-2-ear/target/perf-eap6-app-component-2-ear.ear $EAP6_DIR/server1/standalone/deployments/
}

function start_eap {
  ver=$1
  server=$2

  if [ "$2" = "0" ]; then
    port=8080
    ports=default
  elif [ "$2" = "1" ]; then
    port=8180
    ports=01
  fi
  if [ "$1" = "EAP5" ]; then
    sleep=$EAP5_WAIT
    bootcmd="$EAP5_DIR/jboss-eap-5.1/jboss-as/bin/run.sh -c server${2} -Djboss.messaging.ServerPeerID=${2} -Djboss.service.binding.set=ports-${ports}"
  elif [ "$1" = "EAP6" ]; then
    sleep=$EAP6_WAIT
    bootcmd="$EAP6_DIR/server${2}/bin/standalone.sh -c standalone-full.xml -Djboss.socket.binding.port-offset=${2}00"
  fi

  # give time for any existing AS to shutdown
  sleep 5
  curl  http://localhost:$port > /dev/null 2>&1
  [ $? = 0 ] && fatal "start_eap: server already running on port $port"

  echo starting $1 server server$2 with command "$bootcmd"

  if [ $JBOSS_TERM = "gnome" ]; then
    gnome-terminal --disable-factory -x sh -c "$bootcmd" &
  else
    $bootcmd &
  fi

  if [ "$2" = "0" ]; then
    server0_pid=$!
  else
    server1_pid=$!
  fi

  [ $# = 3 ] && sleep=$3
  echo "$ver server running with pid $! sleep for $sleep"
  sleep $sleep
}

function stop_servers {
  if [ $server0_pid != 0 ]; then
    echo "killing $server0_pid"
    # if not running in a separate terminal the kill only shuts down the shell that started the server
    $EAP5_DIR/jboss-eap-5.1/jboss-as/bin/shutdown.sh -s localhost:1099 -p admin -u admin > /dev/null 2>&1
    $EAP6_DIR/server0/bin/jboss-cli.sh --connect command=:shutdown controller=localhost:9999 > /dev/null 2>&1
    kill "$server0_pid" > /dev/null 2>&1
    server0_pid=0
  fi
  if [ $server1_pid != 0 ]; then
    echo "killing $server1_pid"
    $EAP5_DIR/jboss-eap-5.1/jboss-as/bin/shutdown.sh -s localhost:1199 -p admin -u admin > /dev/null 2>&1
    $EAP6_DIR/server1/bin/jboss-cli.sh --connect command=:shutdown controller=localhost:10099 > /dev/null 2>&1
    kill "$server1_pid" > /dev/null 2>&1
    server1_pid=0
  fi
}

function enable_jts {
  ver=$1
  server=$2
  port=9999

  if [ "$1" = "EAP5" ]; then
    return
  fi

  [ "$2" = "1" ] && port=10099

  start_eap EAP6 $2

  $EAP6_DIR/server$2/bin/add-user.sh admin adm1n

  if  [ "$2" = "0" ]; then
    $EAP6_DIR/server$2/bin/jboss-cli.sh --connect controller=localhost:9999 --user=admin --password=adm1n << JTS
/subsystem=transactions/:write-attribute(name=jts,value=true)
/subsystem=jacorb/:write-attribute(name=transactions,value=on)
JTS
#TODO /subsystem=transactions/:write-attribute(name=node-identifier,value=0)
#/subsystem=jacorb:write-attribute(name="max-threads", value=64)
#pool-size "The size of the request processors thread-pool"
  else
    $EAP6_DIR/server$2/bin/jboss-cli.sh --connect controller=localhost:10099 --user=admin --password=adm1n << JTS
/subsystem=transactions/:write-attribute(name=jts,value=true)
/subsystem=jacorb/:write-attribute(name=transactions,value=on)
JTS
#TODO jacorb.properties /subsystem=transactions/:write-attribute(name=node-identifier,value=1)
#/subsystem=jacorb:write-attribute(name="max-threads", value=64)
  fi
}
    
function enable_hornetq_os {
  ver=$1
  server=$2
  port=9999

  if [ "$ver" = "EAP5" ]; then
    return
  fi

  [ "$server" = "1" ] && port=10099

  start_eap EAP6 $server

  if  [ "$server" = "0" ]; then
      $EAP6_DIR/server${server}/bin/jboss-cli.sh --connect controller=localhost:9999 --user=admin --password=adm1n << JTS
/subsystem=transactions/:write-attribute(name=use-hornetq-store,value=true)
JTS
  else
      $EAP6_DIR/server${server}/bin/jboss-cli.sh --connect controller=localhost:10099 --user=admin --password=adm1n << JTS
/subsystem=transactions/:write-attribute(name=use-hornetq-store,value=true)
JTS
  fi
}

function hornetq_os_enable {
  do_copy $PROD_DIR/standalone-full-hq-server0.xml $EAP6_DIR/server0/standalone/configuration/standalone-full.xml
  do_copy $PROD_DIR/standalone-full-hq-server1.xml $EAP6_DIR/server1/standalone/configuration/standalone-full.xml
  store_type=HornetqStore
}

function hornetq_os_disable {
  do_copy $PROD_DIR/standalone-full-server0.xml $EAP6_DIR/server0/standalone/configuration/standalone-full.xml
  do_copy $PROD_DIR/standalone-full-server1.xml $EAP6_DIR/server1/standalone/configuration/standalone-full.xml
  store_type=FileStore
}

function update_data_dir {
  ver=$1
  server=$2
  enable=$3
  mnt_dev=$4

  ADATA_DIR="$mnt_dev/$ver/data$server"

  if [ "$1" = "EAP5" ]; then
    DATA_DIR="$EAP5_DIR/jboss-eap-5.1/jboss-as/server/server${2}/data"
  elif [ "$1" = "EAP6" ]; then
    DATA_DIR="$EAP6_DIR/server${2}/standalone/data"
  fi

  rm -rf "$DATA_DIR"
  if [ "$enable" = 1 ]; then
    [ -d "$ADATA_DIR" ] || mkdir -p $ADATA_DIR
    rm -rf $ADATA_DIR/*
    ln -s "$ADATA_DIR" "$DATA_DIR"
  fi
}

function patch_jacorb {
  if [ "$1" = "EAP5" ]; then
    do_copy  $PROD_DIR/etc/jacorb.jar.patched $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server${2}/lib/jacorb.jar
  else
    do_copy  $PROD_DIR/etc/jacorb.jar.patched $EAP6_DIR/server${2}/modules/org/jacorb/main/jacorb-2.3.2-redhat-2.jar
  fi
  jacorb_patch=true
}

function unpatch_jacorb {
  if [ "$1" = "EAP5" ]; then
    do_copy $PROD_DIR/etc/jacorb.jar.eap5 $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server${2}/lib/jacorb.jar
  else
    do_copy $PROD_DIR/etc/jacorb.jar.eap6 $EAP6_DIR/server${2}/modules/org/jacorb/main/jacorb-2.3.2-redhat-2.jar
  fi
  jacorb_patch=false
}

function onetest {
  show_header=false
  [ $1 = 0 ] && show_header=true
  calls=$2
  threads=$3
  transactional=true
  enlist=1
  remote=true
  prepareDelay=0
  verbose=true
  html=false

    qs="count=$calls&threads=$threads&verbose=$verbose&prepareDelay=$prepareDelay&enlist=$enlist&remote=$remote&transactional=$transactional&version=$version&html=$html&show_header=$show_header&store_type=$store_type&jacorb_patch=$jacorb_patch"
    echo "qs=$qs"

    curl http://localhost:8080/perf/test -d "$qs" >> $RES_FILE 2>/dev/null

    return 0
}

function test_group {
  start_eap $1 0 0
  start_eap $1 1

  cd $RES_DIR

  version=$1

  if ! test -z "$tests"; then
    sz=${#tests[*]}
    for ((j=0;j<$sz;j++)); do
#      echo    ${tests[${j}]}
      onetest $j ${tests[${j}]}
    done
  else
    echo "WARNING tests called with no test specification"
  fi

  stop_servers
}

function patch_eap {
  for ver in $versions; do
    patch_jacorb $ver 0
    patch_jacorb $ver 1
  done
}

function unpatch_eap {
  for ver in $versions; do
    unpatch_jacorb $ver 0
    unpatch_jacorb $ver 1
  done
}

function env_setup {
  clone_perf_repo
  configure_eap5
  configure_eap6
  update_ear
}

function change_data_dirs {
  for ver in $versions; do
    update_data_dir $ver 0 $1 $2
    update_data_dir $ver 1 $1 $2
  done
}


function run_tests {
  if ! test -z "$tests"; then
    for ver in $versions; do
      test_group $ver
    done

    # reset the array ready for the next test group
    tests=()
    index=0
  fi
}

function cmd_syntax {
cat << HERE
$1. You can find example command files (tests*.txt) in the perftest/etc directory.

A command file should contain lines of the form:"

  print <text> # print a message to standard output"
  versions {EAP5|EAP6} # determines which versions to test against"
  jacorb <patch|unpatch> # apply/remove jacorb patch before running the tests"
  hornetq <enable|disable> # use the hornetq store (if disable then the file store is used"
  data <directory> # put the app server data directory beneath directory"
  # {text} # annotate the command file"
  test [calls] [threads] [transactional] [enlist] [remote] # run a test"

So the general idea is to set a number of config options and then specify a group of tests to
run against that configuration. Then you set more config options followed by another group of
tests specifications etc.

Test output goes to: results/perf.<pid>.tab
HERE
}

function set_option {
  [ ! -n "$1" ] && return
  [[ $1 == \#* ]] && return
  [ $1 != "test" ] && run_tests

  case $1 in
  "test") shift; tests[$index]="$@"; index=$(($index+1)) ;;
  "print") shift; echo "$@";;
  "versions") shift; versions="$@";;
  "jacorb") if [ $2 = "patch" ]; then patch_eap; else unpatch_eap; fi;;
  "hornetq") if [ $2 = "enable" ]; then hornetq_os_enable; else hornetq_os_disable; fi;;
  "data") if [ $2 = "default" ]; then change_data_dirs 0 ""; else change_data_dirs 1 $2; fi;;
  *) cmd_syntax "$2 is an unregcognised option";;
  esac
}

function process_cmds {
  if [ ! -f $1 ]; then
    cmd_syntax "Cannot open command file $1"
    fatal "Cannot open command file $1"
  fi

  [ -d $RES_DIR ] || mkdir -p $RES_DIR
  touch $RES_FILE

  echo "Using command file $1 and writing results to $RES_FILE"

  while read ln ; do
    set_option $ln
  done < "$1"

  run_tests

  # save the server logs
  for ver in $versions; do
    case $ver in
    "EAP5")
      mkdir $RES_DIR/EAP5;
      cp $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server0/log/* $RES_DIR/EAP5;;
    "EAP6")
      mkdir $RES_DIR/EAP6;
      cp $EAP6_DIR/server0/standalone/log/* $RES_DIR/EAP6;;
    esac
  done
}

# Allow the caller to abort the tests
trap 'bail_out' 1 2 3 15

env_setup

# determine which file to use for commands
if [ $# -gt 0 ]; then
  [[ $1 == /* ]] && f=$1 || f="$BASE_DIR/$1"
else
  f=$PROD_DIR/etc/tests.txt
fi

process_cmds $f

do_copy "$f" "$RES_DIR/cmds.$$.tab"
echo "Your results are awaiting your expert analysis in $RES_FILE"
exit 0
