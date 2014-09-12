package ro.tincu.hazelcast_playground;

import com.hazelcast.client.ClientSetAllRequest;
import com.hazelcast.client.spi.ClientProxy;
import com.hazelcast.map.MapEntrySet;
import com.hazelcast.nio.Address;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.util.ThreadUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gabriel on 11.09.2014.
 */
public class SetAllClientInvocationService extends ClientProxy {
    private ClientProxy delegate;
    public SetAllClientInvocationService(String instanceName, ClientProxy delegate){
        super(instanceName, delegate.getServiceName(), delegate.getName());
        this.delegate = delegate;
    }
    @Override
    protected void onDestroy() {
        // TODO the client proxy destroys the near cache here...but we have no near cache
    }

    public<K,V> void setAll(Map<K,V> map){
        Map<Address,MapEntrySet> targets = new HashMap<>();
        for(Map.Entry<K,V> entry : map.entrySet()){
            Data keyData = toData(entry.getKey());
            Data valueData = toData(entry.getValue());
            Address address = getContext().getPartitionService().getPartitionOwner(getContext().
                    getPartitionService().getPartitionId(keyData));
            if(targets.containsKey(address)){
                targets.get(address).add(keyData,valueData);
            } else {
                MapEntrySet newSet = new MapEntrySet();
                newSet.add(valueData,valueData);
                targets.put(address, newSet);
            }
        }
        for(Map.Entry<Address,MapEntrySet> payload : targets.entrySet()){
            try {
                getContext().getInvocationService().invokeOnTarget
                        (new ClientSetAllRequest(delegate.getName(), payload.getValue(), ThreadUtil.getThreadId(), -1),
                                payload.getKey());
            } catch (Exception e){
                // TODO -> which logger do we use here??
            }
        }
    }
}
