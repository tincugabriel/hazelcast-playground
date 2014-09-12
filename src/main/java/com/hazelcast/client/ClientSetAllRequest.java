package com.hazelcast.client;

import com.hazelcast.map.MapEntrySet;
import com.hazelcast.map.MapKeySet;
import com.hazelcast.map.MapService;
import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.hazelcast.security.permission.ActionConstants;
import com.hazelcast.security.permission.MapPermission;
import com.hazelcast.spi.Operation;
import ro.tincu.hazelcast_playground.SetAllBackupOperation;
import ro.tincu.hazelcast_playground.SetAllOperation;

import java.io.IOException;
import java.security.Permission;

/**
 * Created by gabriel on 11.09.2014.
 */
public class ClientSetAllRequest extends ClientRequest implements Portable, SecureRequest {
    protected String name;
    protected long threadId;
    protected long ttl;
    private MapEntrySet entrySet;

    public ClientSetAllRequest(String name, MapEntrySet entrySet, long threadId, long ttl) {
        this.name = name;
        this.entrySet = entrySet;
        this.threadId = threadId;
        this.ttl = ttl;
    }

    @Override
    public String getServiceName() {
        return MapService.SERVICE_NAME;
    }

    @Override
    public int getFactoryId() {
        return 333;
    }

    @Override
    public int getClassId() {
        return 999;
    }

    @Override
    public Permission getRequiredPermission() {
        return new MapPermission(name, ActionConstants.ACTION_PUT);
    }

    @Override
    void process() throws Exception {
        Operation op = new SetAllOperation(name, entrySet);
        //TODO -> this should be the place where you launch the operation on the localhost only !!!
        //TODO -> Alternately, should the api allow, we could sort the entryset  out on the hazelcast instance side
//        clientEngine.
    }

    @Override
    public void write(PortableWriter writer) throws IOException {
        writer.writeUTF("n", name);
        writer.writeLong("t", threadId);
        writer.writeLong("ttl", ttl);
        entrySet.writeData(writer.getRawDataOutput());
    }

    @Override
    public void read(PortableReader reader) throws IOException {
        name = reader.readUTF("n");
        threadId = reader.readLong("t");
        ttl = reader.readLong("ttl");
        entrySet = new MapEntrySet();
        entrySet.readData(reader.getRawDataInput());
    }
}
