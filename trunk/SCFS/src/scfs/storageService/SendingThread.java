package scfs.storageService;

import scfs.directoryService.DirectoryService;
import scfs.directoryService.exceptions.DirectoryServiceException;
import scfs.general.Printer;
import scfs.lockService.LockService;

public class SendingThread extends Thread{

	private DirectoryService diS;
	private LockService loS;
	private IAccessor accessor;
	private SendingQueue queue;
	private DataStatsManager dataManager;
	private ObjectInQueue object;
	private ObjectInQueue nextObject;
	private Object sync;
	private int threadId;
	int cont;

	public SendingThread(DirectoryService diS, LockService loS, SendingQueue queue, 
			DataStatsManager dataManager, int clientId, int threadId, boolean isDepSkyAccessor, String accessKey, String secretKey){
		this.diS = diS;
		this.loS = loS;
		this.queue = queue;
		this.dataManager = dataManager;
		if(isDepSkyAccessor)
			this.accessor = new DepSkyAcessor(clientId);
		else
			this.accessor = new AmazonAccessor(clientId, accessKey, secretKey);
		this.sync = new Object();
		this.object = null;
		this.nextObject = null;
		this.threadId = threadId;
		cont = 0;
	}

	public void run(){

		byte[] hash = null;
		boolean flag = false; 
		while(true){
			try {
				if(object != null){
					Printer.println("-> Sending Thread : sending data: " + object.getFileId(), "vermelho");
					hash = accessor.writeTo(object.getFileId(), object.getData(), object.getKey());
					synchronized (sync) {
						dataManager.setWriteInClouds(object.getFileId(), false, false);
						dataManager.setHash(object.getFileId(), hash);
						diS.commitMetadataBuffer(object.getFileId(), hash);
						if(object.getToRelease()){
							loS.release(object.getFileId());
						}
						if(nextObject == null){
							flag = queue.releaseSendingThread(threadId);
							if(!flag){								
								object = null;
							}
						}else{
							object = nextObject;
							nextObject = null;
						}
					}
				}else{
					Thread.sleep(2000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (DirectoryServiceException e) {
				e.printStackTrace();
			}
		}

	}

	public void setObjectToSend(ObjectInQueue object){
		synchronized (sync) {
			this.object = object;
		}
	}

	public boolean setSameObjectToSend(ObjectInQueue nextObject){
		synchronized (sync) {
			if(this.object != null && this.object.getFileId().equals(nextObject.getFileId())){
				if(this.nextObject!=null && this.nextObject.getToRelease()){
					loS.release(nextObject.getFileId());
				}
				this.nextObject = nextObject;
				cont++;
				return true;
			}else{
				return false;
			}
		}
	}

	public boolean setToRelease(String fileId, boolean toRelease){
		synchronized (sync) {
			if(object != null && object.getFileId().equals(fileId)){
				if(!object.getToRelease() && nextObject == null){
					object.setToRelease(toRelease);
				}else if(nextObject != null && !nextObject.getToRelease()){
					nextObject.setToRelease(toRelease);
				}
				return true;
			}
		}
		return false;
	}


}
