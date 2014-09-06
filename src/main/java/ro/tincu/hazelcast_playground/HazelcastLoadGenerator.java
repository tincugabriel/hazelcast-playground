package ro.tincu.hazelcast_playground;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.*;
import com.hazelcast.map.MapInterceptor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by gabi on 7/23/14.
 */
public class HazelcastLoadGenerator {
    public static final Logger LOGGER = Logger.getLogger(HazelcastLoadGenerator.class);
    public static final HazelcastInstance INSTANCE = Hazelcast.newHazelcastInstance();
    public static final String KEY_TEMPLATE = "map_key_number_%d";
    public static final String VALUE_TEMPLATE = "map_value_number_%d";
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("s","size",true,"Number of elements to iterate through");
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = parser.parse(options, args);
        String optionValue = commandLine.getOptionValue("s", "5000");
        String splitNum = commandLine.getOptionValue("n","1");
        int num = Integer.parseInt(splitNum);
        int size = Integer.parseInt(optionValue);
        Map<String, String> loadMap = new ConcurrentHashMap<>();
        for(int i=0 ; i<size ; i++){
            loadMap.put(String.format(KEY_TEMPLATE,i+(size*num)), String.format(VALUE_TEMPLATE,i+(size*num)));
        }
        scheduledExecutorService.scheduleAtFixedRate(
                new BackgroundRunnable(INSTANCE, INSTANCE.<String,String>getMap("test"), loadMap),
                1000, 1000, TimeUnit.MILLISECONDS);
        synchronized (optionValue){
            optionValue.wait();
        }
    }

    static class BackgroundRunnable implements Runnable {
        private final HazelcastInstance instance;
        private final IMap<String, String> map;
        private final Map<String, String> loadMap;
        public BackgroundRunnable(HazelcastInstance instance, IMap<String, String> map, Map<String,String> loadMap){
            this.instance = instance;
            this.map = map;
            this.loadMap = loadMap;
        }
        @Override
        public void run() {
            try{
                long then = System.currentTimeMillis();
                map.putAll(loadMap);
                LOGGER.info(String.format("Made a bulk update useing putAll for %d elements in %d millisecond",
                        loadMap.size(), System.currentTimeMillis() - then));
            } catch (Exception e){
                LOGGER.warn("Encountered exception while doing bulk update : ",e);
            }
        }
    }
}
