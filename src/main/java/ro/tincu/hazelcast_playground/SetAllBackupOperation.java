package ro.tincu.hazelcast_playground;

import com.hazelcast.map.RecordStore;
import com.hazelcast.map.operation.AbstractMapOperation;
import com.hazelcast.map.record.Record;
import com.hazelcast.map.record.RecordInfo;
import com.hazelcast.nio.IOUtil;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.BackupOperation;
import com.hazelcast.spi.PartitionAwareOperation;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by gabriel on 9/6/14.
 */
public class SetAllBackupOperation extends AbstractMapOperation implements PartitionAwareOperation, BackupOperation {

    private List<Map.Entry<Data,Data>> entries;
    private List<RecordInfo> recordInfos;
    private RecordStore recordStore;

    public SetAllBackupOperation(String name, List<Map.Entry<Data,Data>> entries, List<RecordInfo> recordInfos) {
        super(name);
        this.entries = entries;
        this.recordInfos = recordInfos;
    }

    public SetAllBackupOperation() {
    }

    public void run() {
        int partitionId = getPartitionId();
        recordStore = mapService.getRecordStore(partitionId, name);
        for (int i = 0; i < entries.size(); i++) {
            final RecordInfo recordInfo = recordInfos.get(i);
            final Map.Entry<Data,Data> entry = entries.get(i);
            final Record record = recordStore.putBackup(entry.getKey(), entry.getValue(), -1, false);
            mapService.applyRecordInfo(record, name, recordInfo);
        }
    }

    private void updateSizeEstimator(long recordSize) {
        recordStore.getSizeEstimator().add(recordSize);
    }

    private long calculateRecordSize(Record record) {
        return recordStore.getSizeEstimator().getCost(record);
    }

    @Override
    public Object getResponse() {
        return entries;
    }

    @Override
    public String toString() {
        return "PutAllBackupOperation{" +
                '}';
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);
        final int size = entries.size();
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            final Map.Entry<Data,Data> entry = entries.get(i);
            entry.getKey().writeData(out);
            entry.getValue().writeData(out);
            recordInfos.get(i).writeData(out);
        }
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);
        final int size = in.readInt();
        entries = new ArrayList<Map.Entry<Data,Data>>(size);
        recordInfos = new ArrayList<RecordInfo>(size);
        for (int i = 0; i < size; i++) {
            Map.Entry entry = new AbstractMap.SimpleImmutableEntry<Data, Data>(IOUtil.readData(in), IOUtil.readData(in));
            entries.add(entry);
            final RecordInfo recordInfo = new RecordInfo();
            recordInfo.readData(in);
            recordInfos.add(recordInfo);
        }

    }

}
