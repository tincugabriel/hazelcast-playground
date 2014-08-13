#!/bin/sh



java -cp lib/hazelcast-playground-1.0-SNAPSHOT.jar:lib/log4j-1.2.16.jar -Dlog4j.configuration=file:/tmp/hazelcast-playground-1.0-SNAPSHOT-app-assembly/hazelcast-playground-1.0-SNAPSHOT/conf/logger.properties ro.tincu.hazelcast_playground.App