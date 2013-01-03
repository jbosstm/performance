
EAP 5 testing:
==============

Create two server directories 

	cd XXX/jboss-eap-5.1/jboss-as/server
	cp -r all node1
	cp -r all node2
	export JAVA_HOME=...

If you want to use JTS then update the config on the two server profiles you have just created:

	cd XXX/jboss-eap-5.1/jboss-as/docs/examples/transactions
	ant jts -Dtarget.server.dir=../../../server/node1
	ant jts -Dtarget.server.dir=../../../server/node2

Start both servers (use different terminals)

	cd XXX/jboss-eap-5.1/jboss-as/server
	./jboss-as/bin/run.sh -c node1 -Djboss.messaging.ServerPeerID=1 -Djboss.service.binding.set=ports-default
	./jboss-as/bin/run.sh -c node2 -Djboss.messaging.ServerPeerID=2 -Djboss.service.binding.set=ports-01

Build and deploy the performance testing ear:

	cd wherever
	mvn clean install
	cp perf-ear/target/perf-ear.ear XXX/jboss-eap-5.1/jboss-as/server/node1/deploy/
	cp perf-ear/target/perf-ear.ear XXX/jboss-eap-5.1/jboss-as/server/node2/deploy/

Test the throughput for 100 transactional ejb calls using an HTTP client:

	curl http://localhost:8080/perf-war/PerfTest -d "count=100
	wget http://localhost:8080/perf-war/PerfTest?count=100

EAP 6 testing:
==============

	cd XXX
	cp -r jboss-eap-6.0 server1
	cp -r jboss-eap-6.0 server2

If you want to run in jts mode then edit standalone/configuration/standalone-full.xml in each server
directory you just created and add <jts/> to the <subsystem xmlns="urn:jboss:domain:transactions:1.2">
section. Similarly if you wish to use the hornetq transaction log store then add <use-hornetq-store/>
to the same section

Start both servers (use different terminals)

	./server1/bin/standalone.sh -c standalone-full.xml -Djboss.socket.binding.port-offset=000
	./server2/bin/standalone.sh -c standalone-full.xml -Djboss.socket.binding.port-offset=100

Build and deploy the performance testing ear:

	cd wherever
	mvn clean install

	cp perf-ear/target/perf-ear.ear XXX/server1/standalone/deployments/
	cp perf-ear/target/perf-ear.ear XXX/server1/standalone/deployments/

Test the throughput for 100 transactional ejb calls

	curl http://localhost:8080/perf-war/PerfTest -d "count=100
	wget http://localhost:8080/perf-war/PerfTest?count=100

