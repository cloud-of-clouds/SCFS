package scfs.storageService;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;


public class MemoryCacheManager implements ICacheManager{

	private long maxMemorySize = 2147483648L;
	private ConcurrentHashMap<String, byte[]> memory;
	private LinkedList<String> fileIds;
	private long memoryOccupied = 0;
	private DiskCacheManager diskManager;

	public MemoryCacheManager(int memorySize, DiskCacheManager diskManager){
		this.memory = new ConcurrentHashMap<String, byte[]>();
		this.fileIds = new LinkedList<String>();
		this.diskManager = diskManager;
	}


	public byte[] read(String fileId, int offset, int capacity) {

		if(memory.containsKey(fileId)){
			byte[] data = memory.get(fileId);
			if(capacity == data.length){
				return data;
			}else{
				if(capacity==-1)
					capacity=data.length;
				byte[] dataToRet = new byte[capacity];
				for(int i = offset; i < offset + capacity; i++){
					if(i < data.length)
						dataToRet[i-offset] = data[i];
					else
						i = offset+capacity+1;
				}
				fileIds.remove(fileId);
				fileIds.addFirst(fileId);
				return dataToRet;
			}
		}
		return null;
	}

	public int write(String fileId, byte[] value, int offset) {

		while(memoryOccupied + value.length > maxMemorySize){
			String last = fileIds.getLast();
			diskManager.write(fileId, memory.get(last), 0);
			fileIds.remove(last);
			memory.remove(last);
		}

		byte[] data = null;

		if(!memory.containsKey(fileId)){
			/*data = new byte[offset+value.length];
			for(int i = offset; i < data.length; i++){
				data[i] = value[i-offset];
			}*/
			//data = new byte[value.length];
			memory.put(fileId, value);
			return 0;
		}else{
			data = memory.get(fileId);
			if(offset+value.length > data.length){
				byte[] newData = new byte[(offset+value.length)*2];
				for(int i = 0; i < data.length; i++){
					newData[i] = data[i];
				}
				data = newData;
			}
			for(int i = offset; i < value.length+offset; i++){
				data[i] = value[i-offset];
			}
			fileIds.remove(fileId);
			fileIds.addFirst(fileId);
			memory.put(fileId, data);
		}
		return 0;
	}


	public int truncate(String fileId, int size) {

		if(memory.contains(fileId)){
			byte[] data = memory.get(fileId);
			byte[] newData = new byte[size];
			for(int i = 0; i < newData.length; i++){
				newData[i] = data[i];
			}
			fileIds.remove(fileId);
			fileIds.addFirst(fileId);
			//FIXME: cuidado!!
			memory.put(fileId, data);
		}
		return 0;
	}


	public int delete(String fileId) {
		memory.remove(fileId);
		fileIds.remove(fileId);
		return 0;
	}


	public boolean isInCache(String fileId) {
		return memory.containsKey(fileId);
	}

}
