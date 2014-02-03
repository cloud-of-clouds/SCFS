package scfs.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;

import scfs.directoryService.NodeMetadata;

public class ExternalizableHashMap extends ConcurrentHashMap<String, NodeMetadata> implements Externalizable {

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.size());
		for(Entry<String, NodeMetadata> e : entrySet()){
			out.writeUTF(e.getKey());
			e.getValue().writeExternal(out);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		int len = in.readInt();
		for(int i = 0; i<len ; i++){
			String key = in.readUTF();
			NodeMetadata m = new NodeMetadata();
			m.readExternal(in);
			this.put(key, m);
		}
	}

}