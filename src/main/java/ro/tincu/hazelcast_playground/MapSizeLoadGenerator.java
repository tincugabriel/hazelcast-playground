package ro.tincu.hazelcast_playground;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.apache.log4j.Logger;

import java.util.Random;

/**
 * Created by gabriel on 10.09.2014.
 */
public class MapSizeLoadGenerator {
    private static final Logger LOGGER = Logger.getLogger(MapSizeLoadGenerator.class);
    public static void main(String[] args) throws Exception{
        HazelcastInstance instance = Hazelcast.newHazelcastInstance();
        IMap<Object, Object> map = instance.getMap("test");
        Random random = new Random();
        while(true){
            Thread.sleep(random.nextInt(300)+300);
            LOGGER.info("Size is "+map.size());
        }
    }
}
