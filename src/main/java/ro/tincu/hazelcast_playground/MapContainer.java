package ro.tincu.hazelcast_playground;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.IMap;
import com.hazelcast.map.MapInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Created by gabi on 7/23/14.
 */
public class MapContainer {
    public static void main(String[] args) throws Exception{
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(
                new FileSystemXmlConfig("src/main/resources/hazelcast.xml")
        );
        ICountDownLatch latch = instance.getCountDownLatch("latch");
        latch.trySetCount(2);
        latch.countDown();
        latch.await(10000, TimeUnit.SECONDS);
        System.out.println(String.format("Synchronized at timestamp : %d", System.currentTimeMillis()));
        IMap<String, String> myMap = instance.getMap("myMap");
        myMap.addInterceptor(new MyMapInterceptor());
        myMap.put("foo", "foo");
        myMap.put("bar", "foo");
        myMap.put("baz", "foo");
        myMap.put("quux","foo");
        myMap.put("tilda","foo");
        instance.shutdown();
    }

    static class MyMapInterceptor implements MapInterceptor {
        @Override
        public Object interceptGet(Object o) {
            return o;
        }

        @Override
        public void afterGet(Object o) {
            System.out.println("Did one get!!");
        }

        @Override
        public Object interceptPut(Object o, Object o2) {
            return o2;
        }

        @Override
        public void afterPut(Object o) {
            System.out.println("Did one put!!");
        }

        @Override
        public Object interceptRemove(Object o) {
            return o;
        }

        @Override
        public void afterRemove(Object o) {

        }
    }
}
