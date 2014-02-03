package scfs.storageService;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;


public class DiskCacheManager implements ICacheManager{

	private LinkedList<String> LRUList;
	private String cacheDir;
	private String cache;


	public DiskCacheManager(String cache){
		
		this.cache = cache + File.separator;
		
		this.cacheDir = this.cache + "data directory" + File.separator;
		
		File f = new File(cacheDir);
		if(!f.exists()){
			System.out.println(f.getPath());
			while(!f.mkdir());
		}

		this.LRUList = new LinkedList<String>();

	}

	public byte[] read(String fileId, int offset, int capacity) {

		try {
			File rootCache = new File(cache);
			while(!rootCache.exists())
				rootCache.mkdir();
			File data = new File(cacheDir);
			while(!data.exists())
				data.mkdir();
			File file = new File(cacheDir+fileId);
			if(!file.exists()){
				return null;
			}
			
			if(capacity==-1){
				capacity = (int) file.length();
			}
			
			RandomAccessFile rand = new RandomAccessFile(file, "r");
			rand.seek(offset);
			byte[] toRead =  new byte[capacity];
			rand.read(toRead);
			rand.close();
			if(LRUList.contains(fileId))
				LRUList.remove(fileId);
			LRUList.addFirst(fileId);
			return toRead;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public int write(String fileId, byte[] value, int offset) {

		try {
			File rootCache = new File(cache);
			while(!rootCache.exists())
				rootCache.mkdir();
			File data = new File(cacheDir);
			while(!data.exists())
				data.mkdir();
			File file = new File(cacheDir+fileId);
			while(!file.exists()){
				file.createNewFile();
			}

			RandomAccessFile rand = new RandomAccessFile(file, "rw");
			if(offset+value.length > file.length()){
				rand.setLength(offset+value.length);
			}
			rand.seek(offset);
			rand.write(value);
			//			size = (int) rand.length();
			rand.close();
			if(LRUList.contains(fileId))
				LRUList.remove(fileId);
			LRUList.addFirst(fileId);
			return 0;
		} catch (IOException e) {

			e.printStackTrace();
		}
		return -1;
	}

	public int truncate(String fileId, int size) {

		try {
			File rootCache = new File(cache);
			while(!rootCache.exists())
				rootCache.mkdir();
			File data = new File(cacheDir);
			while(!data.exists())
				data.mkdir();
			File file = new File(cacheDir+fileId);
			while(!file.exists()){
				file.createNewFile();
			}
			RandomAccessFile rand = new RandomAccessFile(file, "rw");
			rand.setLength(size);
			rand.close();
			if(LRUList.contains(fileId))
				LRUList.remove(fileId);
			LRUList.addFirst(fileId);
			return 0;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;

	}

	public int delete(String fileId) {
		File rootCache = new File(cache);
		while(!rootCache.exists())
			rootCache.mkdir();
		File data = new File(cacheDir);
		while(!data.exists())
			data.mkdir();
		File file = new File(cacheDir+fileId);
		while(file.exists()){
			file.delete();
		}
		return 0;
	}

	public boolean isInCache(String fileId) {
		File rootCache = new File(cache);
		while(!rootCache.exists())
			rootCache.mkdir();
		File data = new File(cacheDir);
		while(!data.exists())
			data.mkdir();
		File file = new File(cacheDir+fileId);
		while(!file.exists())
			return false;
		return true;
	}

}
