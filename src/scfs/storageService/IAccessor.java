package scfs.storageService;

import java.util.LinkedList;
import javax.crypto.SecretKey;
import util.Pair;

public interface IAccessor {

	public byte[] readFrom(String fileId, SecretKey key);
	
	public byte[] readMatchingFrom(String fileId, SecretKey key, byte[] hash);
	
	public byte[] writeTo(String fileId, byte[] value, SecretKey key);
	
	public byte[] writeTo(String fileId, byte[] value, SecretKey key, byte[] hash);
	
	public int setPermition(String fileId, String permition, LinkedList<Pair<String,String>> cannonicalIds);
	
	public int delete(String fileId);

	public int garbageCollection(String fileId, int numVersionToKeep);
}
