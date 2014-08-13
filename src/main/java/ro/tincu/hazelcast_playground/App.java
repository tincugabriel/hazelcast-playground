package ro.tincu.hazelcast_playground;

import org.apache.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.LazyReflectiveObjectGenerator;

/**
 * Hello world!
 *
 */
public class App {
    private static final Logger LOGGER = Logger.getLogger(App.class);

    public static void main( String[] args ) throws Exception{
        LOGGER.warn("fooo !! foooo !!");
        Thread.sleep(2000);
        LOGGER.warn("baaaar !! baaar !!");
    }
}
