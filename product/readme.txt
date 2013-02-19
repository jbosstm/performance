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

Some of the EAP5 dependencies are not available via maven so you will need to install them manually by
running the following script:

	./scripts/install-EAP5-dependencies.sh

To run a default set of tests type

	./scripts/tmcmp.sh

To run a single test using default config properties against EAP5 type

	mvn test -P EAP5

Config properties are read in from the following property file:

	src/test/resources/test1.properties

You may override these properties by setting system properties. For example to test against the JTS
version of EAP5 with 200 transactions using a single thread type

	mvn test -P EAP5 -Diterations=200 -Dthreads=1 -Djts=true

Test results are written to

	target/results.txt
====

The remaining text was copied over from the original ant script and will be updated soon.

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

USAGE

mvn test


#mvn -e clean compile exec:java -Dexec.mainClass=com.arjuna.ats.tools.perftest.task.ProductPerformanceTest \
#   -Dexec.args="-p com.arjuna.ats.tools.perftest.task.NarayanaWorkerTask -i 100 -t 5"


# To use a patched jacorb backup ~/.m2/repository/org/jacorb/jacorb/2.3.1.jbossorg-1/jacorb-2.3.1.jbossorg-1.jar and copy over the patched version:
cp ../quickstart/narayana/ejb/perftest/etc/jacorb.jar.patched ~/.m2/repository/org/jacorb/jacorb/2.3.1.jbossorg-1/jacorb-2.3.1.jbossorg-1.jar
