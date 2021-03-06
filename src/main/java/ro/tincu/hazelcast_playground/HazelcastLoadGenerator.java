package ro.tincu.hazelcast_playground;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.proxy.ClientMapProxy;
import com.hazelcast.client.spi.ClientProxy;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.*;
import com.hazelcast.map.MapEntrySet;
import com.hazelcast.map.MapInterceptor;
import com.hazelcast.map.MapService;
import com.hazelcast.map.operation.PutAllOperation;
import com.hazelcast.map.proxy.MapProxyImpl;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.partition.InternalPartitionService;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.OperationService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

import static com.hazelcast.map.MapService.SERVICE_NAME;

/**
 * Created by gabi on 7/23/14.
 */
public class HazelcastLoadGenerator {
    public static final Logger LOGGER = Logger.getLogger(HazelcastLoadGenerator.class);
    public static final HazelcastInstance INSTANCE = HazelcastClient.newHazelcastClient();
    public static final String KEY_TEMPLATE = "map_key_number_%d";
    public static final String VALUE_TEMPLATE = "map_value_number_%d";
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("s","size",true,"Number of elements to iterate through");
        options.addOption("n","number",true,"The default number to multiply the size with and thus generate different keys on different hosts");
        options.addOption("o","optimized",false,"Should the call to update the map in bulk be optimized");
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = parser.parse(options, args);
        String sizeValue = commandLine.getOptionValue("s", "10000");
        String splitNum = commandLine.getOptionValue("n","1");
        boolean optimized = commandLine.hasOption("o");
        int num = Integer.parseInt(splitNum);
        int size = Integer.parseInt(sizeValue);
        Map<String, String> loadMap = new ConcurrentHashMap<>();
        for(int i=0 ; i<size ; i++){
            int postFix = i+(size*num);
            loadMap.put(String.format(KEY_TEMPLATE,postFix), String.format(VALUE_TEMPLATE,postFix));
        }
        scheduledExecutorService.scheduleAtFixedRate(
                new BackgroundRunnable(INSTANCE, INSTANCE.<String,String>getMap("test"), loadMap, optimized),
                1000, 1000, TimeUnit.MILLISECONDS);
        LOGGER.info(String.format("Started cyclic background update thread for %d element, with optimized set to %s," +
                " and the number set to %d",size, optimized, num));
        synchronized (sizeValue){
            sizeValue.wait();
        }
    }

    static class BackgroundRunnable implements Runnable {
        private final ExecutorService executor = Executors.newFixedThreadPool(100);
        private final HazelcastInstance instance;
        private final IMap<String, String> map;
        private final Map<String, String> loadMap;
        private final boolean optimized;
        public BackgroundRunnable(HazelcastInstance instance, IMap<String, String> map, Map<String,String> loadMap, boolean runOptimized){
            this.instance = instance;
            this.map = map;
            this.loadMap = loadMap;
            optimized = runOptimized;
        }
        @Override
        public void run() {
            try{
                if(optimized){
                    SetAllClientInvocationService invocationService = new SetAllClientInvocationService(map.getName(), (ClientProxy) map);
                    invocationService.setAll(loadMap);
                } else {
                    runNormal();
                }
            } catch (Exception e){
                LOGGER.warn("Encountered exception while doing bulk update : ",e);
            }
        }

        private void runNormal(){
            long then = System.currentTimeMillis();
            List<BackgroundMapSet> callables = new ArrayList<>();
            for(Map.Entry<String, String> entry : loadMap.entrySet()){
                callables.add(new BackgroundMapSet(map, entry));
            }
            try {
                executor.invokeAll(callables, 1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e){
                LOGGER.error("Caught exception while waiting for invocation to finish : ",e);
            }
            map.size();
            LOGGER.info(String.format("Made a bulk update using putAll for %d elements in %d milliseconds",
                    loadMap.size(), System.currentTimeMillis() - then));
        }

        private void runOptimized(){
            if(map instanceof MapProxyImpl){
                optimisedAsLocalInstance();
            } else if(map instanceof ClientMapProxy){
                ClientMapProxy<String,String> mapProxy = (ClientMapProxy)map;
            }
        }

        private void optimisedAsLocalInstance() {
            long millis = System.currentTimeMillis();
            MapProxyImpl<String, String> proxyMap = (MapProxyImpl<String, String>)map;
            final NodeEngine nodeEngine = proxyMap.getNodeEngine();
            final MapService mapService = proxyMap.getService();
            InternalPartitionService partitionService = nodeEngine.getPartitionService();
            OperationService operationService = nodeEngine.getOperationService();
            PartitioningStrategy strategy = mapService.getMapContainer(proxyMap.getName()).getPartitioningStrategy();
            List<Future> futures = new LinkedList<Future>();
            Map<Integer, MapEntrySet> entryMap = new HashMap<Integer, MapEntrySet>(nodeEngine.getPartitionService().getPartitionCount());
            for (Map.Entry entry : loadMap.entrySet()) {
                if (entry.getKey() == null) {
                    throw new NullPointerException("Null key not allowed");
                }
                if (entry.getValue() == null) {
                    throw new NullPointerException("Null value not allowed");
                }
                int partitionId = partitionService.getPartitionId(entry.getKey());
                if (!entryMap.containsKey(partitionId)) {
                    entryMap.put(partitionId, new MapEntrySet());
                }
                entryMap.get(partitionId).add(new AbstractMap.SimpleImmutableEntry<Data, Data>(mapService.toData(
                        entry.getKey(),
                        strategy),
                        mapService.toData(entry.getValue())
                ));
            }

            for (final Map.Entry<Integer, MapEntrySet> entry : entryMap.entrySet()) {
                final Integer partitionId = entry.getKey();
                final SetAllOperation op = new SetAllOperation(proxyMap.getName(), entry.getValue());
                op.setPartitionId(partitionId);
                futures.add(operationService.invokeOnPartition(SERVICE_NAME, op, partitionId));
            }

            for (Future future : futures) {
                try {
                    future.get();
                } catch (Exception e){
                    LOGGER.warn("Encountered error during optimized set bulk operation : ",e);
                }
            }
            LOGGER.info(String.format("Optimized setAll operation for %d entries took %d milliseconds",
                    loadMap.size(), System.currentTimeMillis() - millis));
        }
    }

    static class BackgroundMapSet implements Callable<Void>{
        private final IMap<String, String> iMap;
        private final Map.Entry<String, String> entry;

        public BackgroundMapSet(IMap<String, String> iMap, Map.Entry<String, String> entry){
            this.entry = entry;
            this.iMap = iMap;
        }

        @Override
        public Void call() throws Exception {
            iMap.set(entry.getKey(), entry.getValue());
            return null;
        }
    }
}
