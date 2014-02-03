package scfs.directoryService;

import javax.crypto.SecretKey;

public class PrivateNameSpaceStats {

	private String idPath;
	private SecretKey key;

	public PrivateNameSpaceStats(String idPath, SecretKey key) {
		this.idPath = idPath;
		this.key = key;
	}
	
	public String getIdPath() {
		return idPath;
	}
	
	public SecretKey getKey() {
		return key;
	}
	
	
}
