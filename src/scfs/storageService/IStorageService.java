package scfs.storageService;
import javax.crypto.SecretKey;


public interface IStorageService{

	public int writeData(String fileId, byte[] value, int offset);
	
	public byte[] readData(String fileId, int offset, int capacity, SecretKey key, byte[] hash, boolean isPending);
	
	public int truncData(String fileId, int size, SecretKey key, byte[] hash, boolean isToSyncWClouds, boolean isPending);
	
	public int deleteData(String fileId);
	
	public byte[] syncWDisk(String fileId);
	
	public int syncWClouds(String fileId, SecretKey key);
	
	public int updateCache(String fileId, SecretKey key, byte[] hash, boolean isPending);

	public int releaseData(String fileId);
	
}
