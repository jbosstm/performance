Notes for running the performance test:

# create two servers:
cp -rp ~/projects/jbosstm/narayana/jboss-as/build/target/jboss-as-7.2.0.Alpha1-SNAPSHOT/ jts/server1
cp -rp ~/projects/jbosstm/narayana/jboss-as/build/target/jboss-as-7.2.0.Alpha1-SNAPSHOT/ jts/server2
cp -rp ~/projects/jbosstm/narayana/jboss-as/build/target/jboss-as-7.2.0.Alpha1-SNAPSHOT/ jta/server3

# copy over a config that uses jts and the hornetq log store
cp server1-standalone-full.xml jts/server1/standalone/configuration/standalone-full.xml
cp server2-standalone-full.xml jts/server2/standalone/configuration/standalone-full.xml

# or, to run with asynchronous prepare, jts and the hornetq log store:
cp server1-async-prepare-standalone-full.xml jts/server1/standalone/configuration/standalone-full.xml
cp server2-async-prepare-standalone-full.xml jts/server2/standalone/configuration/standalone-full.xml

# start both servers using port offsets:
./server1/bin/standalone.sh -c standalone-full.xml -Djboss.socket.binding.port-offset=000
./server2/bin/standalone.sh -c standalone-full.xml -Djboss.socket.binding.port-offset=100

# Build and deploy the ejbs in their respective servers:

mvn clean install jboss-as:deploy -f application-component-1/pom.xml
mvn clean install jboss-as:deploy -f application-component-2/pom.xml

# start the test (the name param determines how many transactions are run):
curl http://localhost:8080/jboss-as-jts-application-component-1/addCustomer.jsf?name=100
curl http://localhost:8280/jboss-as-jta/addCustomer.jsf?name=100

