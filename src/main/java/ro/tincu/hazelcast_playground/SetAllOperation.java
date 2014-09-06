package ro.tincu.hazelcast_playground;

import com.hazelcast.core.EntryEventType;
import com.hazelcast.map.MapEntrySet;
import com.hazelcast.map.RecordStore;
import com.hazelcast.map.SimpleEntryView;
import com.hazelcast.map.operation.AbstractMapOperation;
import com.hazelcast.map.operation.PutAllBackupOperation;
import com.hazelcast.map.record.Record;
import com.hazelcast.map.record.RecordInfo;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.partition.InternalPartitionService;
import com.hazelcast.spi.BackupAwareOperation;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.PartitionAwareOperation;

import java.io.IOException;
import java.util.*;

/**
 * Created by gabriel on 9/6/14.
 */
public class SetAllOperation extends AbstractMapOperation implements PartitionAwareOperation, BackupAwareOperation {

    private MapEntrySet entrySet;
    private boolean initialLoad = false;
    private List<Map.Entry<Data,Data>> backupEntrySet;
    private List<RecordInfo> backupRecordInfos;

    public SetAllOperation(String name, MapEntrySet entrySet) {
        super(name);
        this.entrySet = entrySet;
    }

    public SetAllOperation(String name, MapEntrySet entrySet, boolean initialLoad) {
        super(name);
        this.entrySet = entrySet;
        this.initialLoad = initialLoad;
    }

    public SetAllOperation() {
    }

    public void run() {
        backupRecordInfos = new ArrayList<RecordInfo>();
        backupEntrySet = new ArrayList<Map.Entry<Data,Data>>();
        int partitionId = getPartitionId();
        RecordStore recordStore = mapService.getRecordStore(partitionId, name);
        Set<Map.Entry<Data, Data>> entries = entrySet.getEntrySet();
        InternalPartitionService partitionService = getNodeEngine().getPartitionService();
        Set<Data> keysToInvalidate = new HashSet<Data>();
        for (Map.Entry<Data, Data> entry : entries) {
            Data dataKey = entry.getKey();
            Data dataValue = entry.getValue();
            if (partitionId == partitionService.getPartitionId(dataKey)) {
                recordStore.set(dataKey, dataValue, -1);
                mapService.interceptAfterPut(name, dataValue);
                keysToInvalidate.add(dataKey);
                if (mapContainer.getWanReplicationPublisher() != null && mapContainer.getWanMergePolicy() != null) {
                    Record record = recordStore.getRecord(dataKey);
                    final SimpleEntryView entryView = mapService.createSimpleEntryView(dataKey,mapService.toData(dataValue),record);
                    mapService.publishWanReplicationUpdate(name, entryView);
                }
                backupEntrySet.add(entry);
                RecordInfo replicationInfo = mapService.createRecordInfo(mapContainer,
                        recordStore.getRecord(dataKey));
                backupRecordInfos.add(replicationInfo);
            }
        }
        invalidateNearCaches(keysToInvalidate);
    }

    protected final void invalidateNearCaches(Set<Data> keys) {
        if (mapService.isNearCacheAndInvalidationEnabled(name)) {
            mapService.invalidateAllNearCaches(name, keys);
        }
    }

    @Override
    public Object getResponse() {
        return true;
    }

    @Override
    public String toString() {
        return "PutAllOperation{" +
                '}';
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);
        out.writeObject(entrySet);
        out.writeBoolean(initialLoad);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);
        entrySet = in.readObject();
        initialLoad = in.readBoolean();
    }

    @Override
    public boolean shouldBackup() {
        return !backupEntrySet.isEmpty();
    }

    public final int getAsyncBackupCount() {
        return mapContainer.getAsyncBackupCount();
    }

    public final int getSyncBackupCount() {
        return mapContainer.getBackupCount();
    }

    @Override
    public Operation getBackupOperation() {
        return new SetAllBackupOperation(name, backupEntrySet, backupRecordInfos);
    }
}
