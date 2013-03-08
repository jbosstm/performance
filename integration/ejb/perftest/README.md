Latest Updates
==============

Build it:

	mvn clean install

Copy artifacts to EAP5 and EAP6 server deploy directories:

	cp perf-eap5/harness/ear/target/ear-1.0.ear XXX/eap-5.1.1/jboss-eap-5.1/jboss-as/server/server0/deploy
	cp perf-eap5/harness/ear2/target/ear2-1.0.ear XXX/eap-5.1.1/jboss-eap-5.1/jboss-as/server/server1/deploy

	cp perf-eap6/application-component-1/target/perf-eap6-app-component-1.war XXX/eap-6.0/server0/standalone/deployments/
	cp perf-eap6/application-component-2-ear/target/perf-eap6-app-component-2-ear.ear XXX/eap-6.0/server1/standalone/deployments/

Start two servers (EAP5 or EAP6 but not both) and send an HTTP request to lauch a test:

	curl http://localhost:8080/perf/test -d "count=10000&threads=24&html=false"

Unattended Testing
==================

Unattended operation has been scripted by running the bash shell script located in scripts/eapcmp.sh (consequently that script is the most accurate set of instructions). If you already have the zip downloads for EAP5 and EAP6 then you can avoid downloading them provided you run the script in a directory that contains the following hierarchy:

	eap-6.0/jboss-eap-6.0.1.zip 
	eap-5.1.1/jboss-eap-5.1.1.zip

The performance test results are saved in a file called ./results/perf.pid.tab and the configuration used to produce those results is stored in a file called results/cmds.pid.tab (where pid is the process id of the shell used to run the script).

To modify what gets tested pass a file argument to eapcmp.sh (type eapcmp.sh -help to discover the syntax of a commands file).

The script starts servers using gnome-terminal (to launch them in the current terminal set JBOSS_TERM to something other than gnome)

Manual EAP 5 testing:
=====================

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

Manual EAP 6 testing:
=====================

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

