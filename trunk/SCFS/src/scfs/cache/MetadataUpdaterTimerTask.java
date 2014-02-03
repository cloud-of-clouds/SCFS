package scfs.cache;

import java.util.TimerTask;

import scfs.directoryService.DirectoryService;
import scfs.directoryService.NodeMetadata;
import scfs.directoryService.exceptions.DirectoryServiceException;

public class MetadataUpdaterTimerTask extends TimerTask{

	private DirectoryService directoryService;
	private NodeMetadata metadata;
	private MetadataCache cache;
	private boolean commit;
	private boolean removed;
	private String path;
	private long updaterID;

	public MetadataUpdaterTimerTask(String path, NodeMetadata metadata, DirectoryService directoryService, MetadataCache cache) {
		this.metadata = metadata;
		this.directoryService = directoryService;
		this.cache = cache;
		this.commit = false;
		this.removed = false;
		this.path = path;
		this.updaterID = System.currentTimeMillis();
	}

	public void setCommit(boolean commit){
		synchronized (this) {
			this.commit = commit;
		}
	}

	public void setRemoved(boolean removed){
		synchronized (this) {
			this.removed = removed;
		}
	}

	public String getPath(){
		return path;
	}

	public NodeMetadata getMetadata(){
		return metadata;
	}


	@Override
	public void run() {
		try {	
			synchronized (this) {
				if(commit && !removed){
					directoryService.updateMetadata(path, metadata);
				}else{}
			}
			cache.removeFromCache(metadata.getPath());
		} catch (DirectoryServiceException e) {
			e.printStackTrace();
		}
	}

	public long getID() {
		return updaterID;
	}

}
