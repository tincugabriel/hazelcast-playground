package ro.tincu.hazelcast_playground;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.*;
import com.hazelcast.map.MapInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Created by gabi on 7/23/14.
 */
public class MapContainer {
    public static void main(String[] args) throws Exception{
        Config cfg = new FileSystemXmlConfig("src/main/resources/hazelcast.xml");
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
        ICountDownLatch latch = instance.getCountDownLatch("latch");
        latch.trySetCount(5);
        latch.countDown();
        latch.await(10000, TimeUnit.SECONDS);
        ILock lock = instance.getLock("interceptorLock");
        System.out.println(String.format("Synchronized at timestamp : %d", System.currentTimeMillis()));
        IMap<Integer, String> myMap = instance.getMap("myMap");
        if(lock.tryLock()){
            myMap.addLocalEntryListener(new MyEntryListener());
        }
        myMap.put(1, "foo");
        myMap.put(2, "bar");
        myMap.put(3, "baz");
        myMap.put(4, "quux");
        myMap.put(5,"tilda");
        myMap.put(6,"epsilon");
        myMap.put(7,"lambda");
        myMap.put(8,"omega");
        myMap.put(9,"alpha");

        myMap.get(1);
        myMap.get(2);
        myMap.get(3);
        myMap.get(4);
        myMap.get(5);
        myMap.get(6);
        myMap.get(7);
        myMap.get(8);
        myMap.get(9);
        latch = instance.getCountDownLatch("secondLatch");
        latch.trySetCount(5);
        latch.countDown();
        latch.await(10000, TimeUnit.SECONDS);
        instance.shutdown();
    }

    static class MyEntryListener implements EntryListener<Integer, String>{
        @Override
        public void entryAdded(EntryEvent<Integer, String> event) {
            System.out.println("Got entry value "+event.getValue());
        }

        @Override
        public void entryRemoved(EntryEvent<Integer, String> event) {

        }

        @Override
        public void entryUpdated(EntryEvent<Integer, String> event) {

        }

        @Override
        public void entryEvicted(EntryEvent<Integer, String> event) {

        }
    }
}
