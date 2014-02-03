package scfs.storageService;

import java.io.Serializable;


public class DataStats implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6900093748194532598L;
	private String fileId;
	private boolean writeInClouds;
	private boolean writeInDisk;
	private byte[] hash;
	private int size;
	
	public DataStats(String fileId){		
		this.fileId = fileId;
		this.writeInClouds = true;
		this.writeInDisk = false;
		this.hash = new byte[0];
		this.size = 0;
	}
	
	public String getFileId(){
		return this.fileId;
	}
	
	public boolean getWriteInClouds(){
		return this.writeInClouds;
	}
	
	public boolean getWriteInDisk(){
		return this.writeInDisk;
	}
	
	public byte[] getHash(){
		return this.hash;
	}
	
	public int getSize(){
		return this.size;
	}
	
	public void setWriteInClouds(boolean writeInClodus){
		this.writeInClouds = writeInClodus;
	}
	
	public void setWriteInDisk(boolean writeInDisk){
		this.writeInDisk = writeInDisk;
	}
	
	public void setHash(byte[] hash){
		this.hash = hash;
	}
	
	public void setSize(int size){
		this.size = size;
	}
}
