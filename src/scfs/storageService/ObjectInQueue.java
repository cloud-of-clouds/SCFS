package scfs.storageService;

import javax.crypto.SecretKey;

public class ObjectInQueue {

	private String fileId;
	private byte[] data;
	private SecretKey key;
	private boolean toRelease;
	
	public ObjectInQueue(String fileId, byte[] data, SecretKey key, boolean toRelease){
		this.fileId = fileId;
		this.data = data;
		this.key = key;
		this.toRelease = toRelease;
	}
	
	public String getFileId(){
		return fileId;
	}
	
	public byte[] getData(){
		return data;
	}
	
	public SecretKey getKey(){
		return key;
	}
	
	public boolean getToRelease(){
		return toRelease;
	}
	
	public void setData(byte[] data){
		this.data = data;
	}
	
	public void setKey(SecretKey key){
		this.key = key;
	}
	
	public void setToRelease(boolean toRelease){
		this.toRelease = toRelease;
	}
}
