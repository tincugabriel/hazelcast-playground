#!/bin/sh



java -cp lib/hazelcast-playground-1.0-SNAPSHOT.jar:lib/log4j-1.2.16.jar:lib/annotations-1.3.2.jar:lib/commons-cli-1.2.jar:lib/hazelcast-3.2.3.jar -Xmx128m -Dhazelcast.logging.type=log4j -Dlog4j.configuration=file:/tmp/target/hazelcast-playground-1.0-SNAPSHOT/conf/size-logger.properties ro.tincu.hazelcast_playground.MapSizeLoafGenerator