package scfs.storageService;

public interface ICacheManager {

	public byte[] read(String fileId, int offset, int capacity);
	
	public int write(String fileId, byte[] value, int offset);
	
	public int truncate(String fileId, int size);
	
	public int delete(String fileId);
	
	public boolean isInCache(String fileId);
}
