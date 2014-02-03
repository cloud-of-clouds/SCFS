package scfs.cache;

import scfs.directoryService.NodeMetadata;

public class MetadataCacheEntry {
	private NodeMetadata metadata;
	private long time;
	private boolean lockState;
	private long leaseTime;
	private String path;
	
	public MetadataCacheEntry(NodeMetadata metadata, long time, boolean lockState, long leaseTime) {
		this.path = new String(metadata.getId_path());
		this.metadata = metadata;
		this.time = time;
		this.lockState = lockState;
		this.leaseTime = leaseTime;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public void setUpdated(NodeMetadata metadata) {
		this.metadata.setC_r(metadata.getC_r());
		this.metadata.setC_w(metadata.getC_w());
		this.metadata.setNodeType(metadata.getNodeType());
		this.metadata.setId_path(metadata.getId_path());
		this.metadata.setKey(metadata.getKey());
		this.metadata.setName(metadata.getName());
		this.metadata.setParent(metadata.getParent());
		this.metadata.setStats(metadata.getStats());
	}

	public long getTime() {
		return time;
	}

	public long getLeaseTime() {
		return leaseTime;
	}

	public NodeMetadata getMetadata() {
		return metadata;
	}
	
	public boolean getLockState(){
		return lockState;
	}
	
	public String getInitialPath(){
		return path;
	}
}