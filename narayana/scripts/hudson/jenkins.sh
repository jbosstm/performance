#!/bin/bash -e


function build_narayana {
    if [ -z $BUILD_NARAYANA ] || [ $BUILD_NARAYANA != "n" ];
    then
        rm -rf tmp
        mkdir tmp
        NARAYANA_REPO=${NARAYANA_REPO:-jbosstm}
        NARAYANA_BRANCH="${NARAYANA_BRANCH:-$GIT_BRANCH}"
        git clone https://github.com/${NARAYANA_REPO}/narayana.git -b ${NARAYANA_BRANCH} tmp
        cd tmp
        git fetch
        ./build.sh clean install -DskipTests
        cd ..
    fi
}

function preamble {
  export JVM_ARGS="-DMAX_ERRORS=10"
  os=`uname -a`
  ptype=`cat /proc/cpuinfo | grep "model name" | head -1`
  pcnt=`cat /proc/cpuinfo | grep processor | wc -l`
  date >> $1
  echo "Platform: $os" >> $1
  echo "Processor: $ptype" >> $1
  echo "Number of Cores: $pcnt" >> $1
  java -version >> $1
  echo -e "Blog Text\n=========" >> $1
}

function bm {
  echo "NEXT RUN using $JMHARGS"
  narayana/scripts/hudson/benchmark.sh "ArjunaJTA/jta" "$2" "$3"
  [ $? = 0 ] || res=1
  cat benchmark-output.txt >> $1
  echo "RUN status: $res"
  return $res
}

function urlencode {
    local LANG=C
    local length="${#1}"
    for (( i = 0; i < length; i++ )); do
        local c="${1:i:1}"
        case $c in
            [a-zA-Z0-9.~_-]) printf "$c" ;;
            *) printf '%%%02X' "'$c" ;;
        esac
    done
}

function publish_bm {
  rm -rf tmp2
  git clone https://github.com/jbosstm/artifacts tmp2
  cp $1 $2 tmp2/jobs/tm-comparison
  cd tmp2
  git add -u
  host=`hostname`
  tm=`date`
  git commit -m "Generated on host $host ($tm) using tag $GIT_BRANCH"
  GT=$(urlencode ${GITHUB_TOKEN})
  git push https://jbosstm-bot:${GT}@github.com/jbosstm/artifacts.git master
}

build_narayana

res=0 
if [ -v OVERRIDE_NARAYANA_VERSION ]; then
  MAVEN_OVERRIDE_NARAYANA_VERSION="-Dnarayana.version=${OVERRIDE_NARAYANA_VERSION}"
fi

./build.sh -f narayana/pom.xml clean package -DskipTests $MAVEN_OVERRIDE_NARAYANA_VERSION

rm -f bm-output.txt benchmark-output.txt benchmark.png

if [ -z ${THREAD_COUNTS+x} ]
then
  THREAD_COUNTS="1 10 50 100 300 400"
fi
if [ -z ${RUN_DURATION+x} ]
then
  RUN_DURATION=100
fi

if [ -z ${COMPARISON+x} ] 
then
  COMPARISON="io.narayana.perf.product.*Comparison.*"
  COMPARISON_COUNT=5
fi

for i in $THREAD_COUNTS
do
  if [ -z "${JMHARGS}" ] ; then
   JMHARGS="-t $i -r 30 -f 3 -wi 5 -i 5"
  fi
  JMHARGS="-t $i ${JMHARGS/-t*-r/ -r}" bm bm-output.txt $COMPARISON $COMPARISON_COUNT
done

cp bm-output.txt benchmark-output.txt
preamble benchmark-output.txt

if [[ $(uname) == CYGWIN* ]]
then
  separator=";"
else
  separator=":"
fi
java -Xms4096m -Xmx4096m -Danonymize=true -classpath "narayana/ArjunaJTA/jta/target/classes"$separator"narayana/ArjunaJTA/jta/target/benchmarks.jar" io.narayana.perf.product.ReportGenerator bm-output.txt $BM_LINE_PATTERN $PATTERN2 >> benchmark-output.txt
cat benchmark-output.txt

if [ -z ${DO_NOT_PUBLISH} ]
then
  publish_bm benchmark-output.txt benchmark.png
fi

exit $res
