Work in progress

JBoss, Home of Professional Open Source
Copyright 2012, Red Hat Middleware LLC, and individual contributors
as indicated by the @author tags.
See the copyright.txt in the distribution for a
full listing of individual contributors.
This copyrighted material is made available to anyone wishing to use,
modify, copy, or redistribute it subject to the terms and conditions
of the GNU Lesser General Public License, v. 2.1.
This program is distributed in the hope that it will be useful, but WITHOUT A
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License,
v.2.1 along with this distribution; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
MA  02110-1301, USA.

(C) 2012
@author JBoss Inc.

Project for comparing performance of various transaction products.

Clone the project:

	git clone xxx
	cd performance/product

and run a default set of tests by typing

	./scripts/tmcmp.sh

On slower machines some of thsee tests can take a good while to complete so read on if you want to just
run a subset of the performance tests. In particular the storage media will have a significant effect on
performance.

Some of the 4.6.1 dependencies are not available via maven so the test script first checks that they are in
your local repo and manually installs them if appropriate. Make sure your M2_HOME env variable is set. You
also need to be on the VPN some dependencies are downloaded from http://repository.jboss.org/maven2

To run a single test using default config properties against EAP6 type

	mvn test -P EAP6

Config properties are read in from the following property file:

	src/test/resources/test1.properties

You may override these properties by setting system properties. For example to test against the JTS
version of EAP5 with 200 transactions using a single thread with a non standard location for the object
store (handy if you have access to a faster storage such as an SSD) type

	mvn test -P EAP5 -Diterations=200 -Dthreads=1 -Djts=true objectStoreDir=/mnt

Test results are written to

	target/results.txt

There is also an option to test against the latest revision of Narayana using the java ORB but you
will will need to install it into your local maven repo yourself:

1) clone the master branch https://github.com/jbosstm/narayana.git
2) ./build.sh clean install -DskipTests=true

Alternatively if you already have the jar you can install it into your local repo using:

mvn install:install-file  -Dfile=<location of narayana-jts-idlj-5.0.0.M2-SNAPSHOT.jar> \
                          -DgroupId=org.jboss.narayana.jts \
                          -DartifactId=narayana-jts-idlj \
                          -Dversion=5.0.0.M2-SNAPSHOT \
                          -Dpackaging=jar \
                          -DlocalRepositoryPath=$M2_REPO
Now test it:

mvn test -P EAP6-JDKORB -Diterations=2000 -Dthreads=10 -Djts=true -DobjectStoreDir=/mnt

====

The remaining text in this readme was copied over from the original ant version of the project and
needs updadating:

OVERVIEW
--------
    Build file for comparing performance of various transaction products.

1. Download required jars
=========================
To test a product download the relevant libraries and put them in the lib directory:

Atomikos release 3.7.0
lib/atomikos/transactions-osgi.jar

Bitronix release 2.1.0
lib/btm/btm-2.1.0.jar
lib/btm/lib/slf4j-api-1.6.0.jar
lib/btm/lib/slf4j-log4j12-1.6.0.jar
lib/btm/lib/log4j-1.2.14.jar 

JOTM release 2.2.1
Copy the contents of the lib dir from JOTM distribution to 
lib/jotm

JBossTS jars are sourced from the build

The compile ant target below currently excludes all products (except JBossTS) - once
you have the available jars for a product remember to remove the exclude line.

3. Adding suport for a new product
==================================
The following steps are needed to integrate a new transaction product:

- obtain the required jars and add them to the classpath
- write a class that extends the abstract class com.arjuna.ats.tools.perftest.task.WorkerTask

4. Testing a product
====================
To test particular products specify them on the command line with a -p flag (comma separated). For example:

        ant -Dargs="-p com.arjuna.ats.tools.perftest.task.AtomikosWorkerTask,com.arjuna.ats.tools.perftest.task.NarayanaWorkerTask,com.arjuna.ats.tools.perftest.task.BitronixWorkerTask,com.arjuna.ats.tools.perftest.task.JotmWorkerTask -i 100 -t 5"

will test Atomikos, JBossTS, Bitronix and JOTM and commit 100 transactions using 5 threads for each product.
