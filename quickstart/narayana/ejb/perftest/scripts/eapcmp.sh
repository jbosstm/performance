#!/bin/bash

BASE_DIR=`pwd`
RES_DIR=$BASE_DIR/results/$$
RES_FILE=$RES_DIR/perf.$$.tab
EAP5_ZIP="http://download.devel.redhat.com/released/JBEAP-5/5.1.1/zip/jboss-eap-5.1.1.zip"
EAP6_ZIP="http://download.devel.redhat.com/released/JBEAP-6/6.0.1/zip/jboss-eap-6.0.1.zip "
PERF_REPO=https://github.com/jbosstm/performance
PERF_REPO=https://github.com/mmusgrov/performance
EAP5_DIR=$BASE_DIR/eap-5.1.1
EAP6_DIR=$BASE_DIR/eap-6.0
EAP5_WAIT=60
EAP6_WAIT=20
file_store=1
jacorb_patch=0
version="-"
versions="EAP6 EAP5"
tests=()
index=0

simulate=0

[ $JBOSS_TERM ] || JBOSS_TERM=gnome 

server0_pid=0
server1_pid=0

function fatal {
    echo "fatal: $1" | tee -a $RES_FILE
    [ -f "$2" ] && cat $2

    stop_servers
    exit 1
}

function do_copy {
  if [ -d $1 ]; then
    cp -r $1 $2
  else
   cp $1 $2
  fi

  [ $? = 0 ] || fatal "do_copy: $1 $2 failed"
}

function configure_eap5 {
  [ -d "$EAP5_DIR" ] || mkdir -p $EAP5_DIR
  cd $EAP5_DIR

  if [ ! -d jboss-eap-5.1 ]; then
    if [ ! -f jboss-eap-5.1.1.zip ]; then
      wget $EAP5_ZIP
      [ $? = 0 ] || fatal "configure_eap5: wget $EAP5_ZIP failed"
    fi
    if [ ! -d jboss-eap-5.1/jboss-as/server/ ]; then
      unzip jboss-eap-5.1.1.zip
      [ $? = 0 ] || fatal "configure_eap5: unzip EAP5 failed"
    fi
    cd jboss-eap-5.1/jboss-as/server
    echo "admin=admin" >> all/conf/props/jmx-console-users.properties
    do_copy all server0
    do_copy all server1
    cd ../docs/examples/transactions/
    ant jts -Dtarget.server.dir=../../../server/server0
    [ $? = 0 ] || fatal "enable EAP5 jts failed for server0"
    ant jts -Dtarget.server.dir=../../../server/server1
    [ $? = 0 ] || fatal "enable EAP5 jts failed for server1"
    do_copy  $EAP5_DIR/jboss-eap-5.1/jboss-as/server/all/lib/jacorb.jar $BASE_DIR/performance/quickstart/narayana/ejb/perftest/etc/jacorb.jar.eap5
  fi

  rm $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server0/log/*
  rm $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server1/log/*
}

function configure_eap6 {
  [ -d "$EAP6_DIR" ] || mkdir -p $EAP6_DIR
  cd $EAP6_DIR

  if [ ! -d jboss-eap-6.0 ]; then
    if [ ! -f jboss-eap-6.0.1.zip ]; then
      wget $EAP6_ZIP
      [ $? = 0 ] || fatal "wget $EAP6_ZIP failed"
    fi

    unzip jboss-eap-6.0.1.zip
    [ $? = 0 ] || fatal "unzip EAP6 failed"

    do_copy jboss-eap-6.0 server0
    do_copy jboss-eap-6.0 server1

    do_copy  $EAP6_DIR/jboss-eap-6.0/modules/org/jacorb/main/jacorb-2.3.2-redhat-2.jar $BASE_DIR/performance/quickstart/narayana/ejb/perftest/etc/jacorb.jar.eap6

    enable_jts EAP6 0
    enable_jts EAP6 1
    stop_servers

    do_copy  $EAP6_DIR/server0/standalone/configuration/standalone-full.xml $BASE_DIR/performance/quickstart/narayana/ejb/perftest/standalone-full-server0.xml
    do_copy  $EAP6_DIR/server1/standalone/configuration/standalone-full.xml $BASE_DIR/performance/quickstart/narayana/ejb/perftest/standalone-full-server1.xml

    enable_hornetq_os EAP6 0
    enable_hornetq_os EAP6 1
    stop_servers

    do_copy  $EAP6_DIR/server0/standalone/configuration/standalone-full.xml $BASE_DIR/performance/quickstart/narayana/ejb/perftest/standalone-full-hq-server0.xml
    do_copy  $EAP6_DIR/server1/standalone/configuration/standalone-full.xml $BASE_DIR/performance/quickstart/narayana/ejb/perftest/standalone-full-hq-server1.xml
  fi

  rm $EAP6_DIR/server0/standalone/log/*
  rm $EAP6_DIR/server1/standalone/log/*
}

function clone_perf_repo {
  if [ ! -d $BASE_DIR/performance/quickstart/narayana/ejb/perftest ]; then
    cd $BASE_DIR
    git clone $PERF_REPO
    [ $? = 0 ] || fatal "clone performance repo failed"
  fi
}

function update_ear {
  cd $BASE_DIR/performance/quickstart/narayana/ejb/perftest
  [ $? = 0 ] || fatal "perftest dir doesn't exist"

  if [ ! -f perf-ear/target/perf-ear.ear ]; then
    mvn clean install
    [ $? = 0 ] || fatal "perftest clean install failed"
  fi

  do_copy perf-ear/target/perf-ear.ear $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server0/deploy/perf-ear.ear
  do_copy perf-ear/target/perf-ear.ear $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server1/deploy/perf-ear.ear

  do_copy perf-ear/target/perf-ear.ear $EAP6_DIR/server0/standalone/deployments/
  do_copy perf-ear/target/perf-ear.ear $EAP6_DIR/server1/standalone/deployments/
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
  else
    $EAP6_DIR/server$2/bin/jboss-cli.sh --connect controller=localhost:10099 --user=admin --password=adm1n << JTS
/subsystem=transactions/:write-attribute(name=jts,value=true)
/subsystem=jacorb/:write-attribute(name=transactions,value=on)
JTS
#TODO jacorb.properties /subsystem=transactions/:write-attribute(name=node-identifier,value=1)
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
  do_copy $BASE_DIR/performance/quickstart/narayana/ejb/perftest/standalone-full-hq-server0.xml $EAP6_DIR/server0/standalone/configuration/standalone-full.xml
  do_copy $BASE_DIR/performance/quickstart/narayana/ejb/perftest/standalone-full-hq-server1.xml $EAP6_DIR/server1/standalone/configuration/standalone-full.xml
  file_store=0
}

function hornetq_os_disable {
  do_copy $BASE_DIR/performance/quickstart/narayana/ejb/perftest/standalone-full-server0.xml $EAP6_DIR/server0/standalone/configuration/standalone-full.xml
  do_copy $BASE_DIR/performance/quickstart/narayana/ejb/perftest/standalone-full-server1.xml $EAP6_DIR/server1/standalone/configuration/standalone-full.xml
  file_store=1
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
    do_copy  $BASE_DIR/performance/quickstart/narayana/ejb/perftest/etc/jacorb.jar.patched $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server${2}/lib/jacorb.jar
  else
    do_copy  $BASE_DIR/performance/quickstart/narayana/ejb/perftest/etc/jacorb.jar.patched $EAP6_DIR/server${2}/modules/org/jacorb/main/jacorb-2.3.2-redhat-2.jar
  fi
  jacorb_patch=1
}

function unpatch_jacorb {
  if [ "$1" = "EAP5" ]; then
    do_copy $BASE_DIR/performance/quickstart/narayana/ejb/perftest/etc/jacorb.jar.eap5 $EAP5_DIR/jboss-eap-5.1/jboss-as/server/server${2}/lib/jacorb.jar
  else
    do_copy $BASE_DIR/performance/quickstart/narayana/ejb/perftest/etc/jacorb.jar.eap6 $EAP6_DIR/server${2}/modules/org/jacorb/main/jacorb-2.3.2-redhat-2.jar
  fi
  jacorb_patch=0
}

function onetest {
    calls=$1
    threads=$2
    transactional=$3
    enlist=$4
    remote=$5

    prepareDelay=0
    verbose=0

    if [ $simulate = 1 ]; then
      printf "%9s %11s %9s %9s %9s %9s %11s %9s %9s\n" $version $throughput $calls $jacorb_patch $file_store $threads $transactional $enlist $remote | tee -a $RES_FILE
      return 0
    fi
    qs="count=$calls&verbose=$verbose&prepareDelay=$prepareDelay&enlist=$enlist&remote=$remote&transactional=$transactional"
    echo "qs=$qs"

    for (( i=1; i<=$threads; i++ )); do
        if [ $i = $threads ]; then 
           curl http://localhost:8080/perf-war/PerfTest -d "$qs" > "res$i" 2>/dev/null
           proc[$i]=0
        else
           curl http://localhost:8080/perf-war/PerfTest -d "$qs" > "res$i" 2>/dev/null &
           proc[$i]=$!
        fi
    done

    tot=0
    for (( i=1; i<=$threads; i++ )); do
        pid=${proc[$i]}
        [ $pid != 0 ] && wait $pid

        cat "res$i" |grep -l html > /dev/null
        [ $? = 1 ] || fatal "run $i $1 $2" "res$i"; # failed

         v=$(cat res$i)
         val=$(echo "$v" | cut -f1 -d\ )

         [[ "$val" =~ ^-?[0-9]+$ ]] || echo "ERROR: servlet didn't return a number: value=$val qs=$qs" | tee -a $RES_FILE
         tot=`expr $tot + $v`
    done

    throughput=$tot
    printf "%9s %11s %9s %9s %9s %9s %11s %9s %9s\n" $version $throughput $calls $jacorb_patch $file_store $threads $transactional $enlist $remote | tee -a $RES_FILE

    return 0
}

function test_group {
  if [ $simulate != 1 ]; then
    start_eap $1 0 0
    start_eap $1 1
  fi

  cd $RES_DIR

  version=$1

  if ! test -z "$tests"; then
    sz=${#tests[*]}
    for ((j=0;j<$sz;j++)); do
#      echo    ${tests[${j}]}
      onetest ${tests[${j}]}
    done
  else
    echo "WARNING tests called with no test specification"
  fi

  if [ $simulate != 1 ]; then
    stop_servers
  fi
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
$1. You can find example command files (tests*.txt) in the perftest directory.

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

  [ -d $RES_DIR ] || mkdir $RES_DIR
  touch $RES_FILE

  echo "Using command file $1 and writing results to $RES_FILE"

  printf "%9s %11s %9s %9s %9s %9s %9s %9s %9s\n" "Version" "Throughput" "Calls" "Patched" "FileStore" "Threads" "Transaction" "Enlist" "Remote" | tee -a $RES_FILE

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
      cp $EAP6_DIR/server0/standalone/log/* $RES_DIR/EAP6;
    esac
  done
}

env_setup

# determine which file to use for commands
if [ $# -gt 0 ]; then
  [[ $1 == /* ]] && f=$1 || f="$BASE_DIR/$1"
else
  f=$BASE_DIR/performance/quickstart/narayana/ejb/perftest/etc/tests.txt
fi

process_cmds $f

do_copy "$f" "$RES_DIR/cmds.$$.tab"
echo "Your results are awaiting your expert analysis in $RES_FILE"
exit 0
