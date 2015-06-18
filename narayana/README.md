
The maven poms in the benchmark repository track narayana master (https://github.com/jbosstm/narayana.git) which must be in your local maven repository. If you do not have it already then clone and build it now:

    git clone https://github.com/jbosstm/narayana.git
    cd narayana
    ./build.sh clean install

This step will have populated maven with the artifacts you will need for building the benchmarks.

Now clone and build the narayana performance git repository:

    cd ..
    git clone https://github.com/jbosstm/performance
    cd performance/narayana
    mvn clean install -DskipTests

Many benchmarks also double up as junit tests so if you just want to validate that the benchmark runs then execute it as a surefire test. For example:

    mvn -f ArjunaJTA/jta/pom.xml -Dtest=com.arjuna.ats.jta.xa.performance.VolatileStoreBenchmark test

Each maven module build produces a benchmarks.jar file in the target directory which contains the benchmark code and the dependencies required by the benchmark. Just run this jar to execute a benchmark. To see what options are available pass the -help argument to any benchmark jar, for example:

    java -jar ./ArjunaJTA/jta/target/benchmarks.jar -help

For example, to run the VolatileStore benchmarks type the following:

    java  -jar ./ArjunaJTA/jta/target/benchmarks.jar com.arjuna.ats.jta.xa.performance.VolatileStoreBenchmark.* -i 1 -wi 2 -f 1 -t 2 -r 10
 
Here we have overridden the defaults and specified "-i 1 -wi 2 -f 1 -t 2 -r 10" which means:
  * run one iteration (-i 1) with 2 warm up cycles (-wi 2) and 1 fork (-f 1);
  * use 2 threads (-t 2) to execute the benchmark code;
  * and run the benchmark for 10 seconds (-r 10)

We also passed in a wild card (.\*) to say run all benchmark methods contained in the class VolatileStoreBenchmark. 

It is straightforward to write your own benchmark code, just tell the framework which methods are benchmarks by annotating them with @Benchmark. There are plenty of examples in this git repository.

