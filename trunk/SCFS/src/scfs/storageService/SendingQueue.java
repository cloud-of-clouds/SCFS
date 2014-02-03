package scfs.storageService;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.SecretKey;

import scfs.directoryService.DirectoryService;
import scfs.lockService.LockService;

public class SendingQueue {

	private ConcurrentHashMap<Integer, SendingThread> sendingThreads;
	private ConcurrentHashMap<Integer, Boolean> activeThreads;
	private ConcurrentHashMap<String, ObjectInQueue> objectsToSend;
	private ConcurrentLinkedQueue<String> queue;
	private ConcurrentHashMap<Integer, String> activeObjects;
	private LockService loS;


	public SendingQueue(DataStatsManager dataManager, DirectoryService diS, LockService loS, int numOfThreads, int clientId, boolean isDepSkyAccessor, String accessKey, String secretKey){
		this.sendingThreads = new ConcurrentHashMap<Integer, SendingThread>();
		this.objectsToSend = new ConcurrentHashMap<String, ObjectInQueue>();
		this.activeThreads = new ConcurrentHashMap<Integer, Boolean>();
		this.queue = new ConcurrentLinkedQueue<String>();
		this.activeObjects = new ConcurrentHashMap<Integer, String>();
		this.loS = loS;
		for(int i = 0; i < numOfThreads; i++){
			sendingThreads.put(i, new SendingThread(diS, loS, this, dataManager, clientId, i, isDepSkyAccessor, accessKey, secretKey));
			sendingThreads.get(i).start();
			activeThreads.put(i, true);
		}		

	}

	public void addSendingObject(String fileId, byte[] data, SecretKey key){
		ObjectInQueue obj = null;
		if(queue.contains(fileId)){
			obj = objectsToSend.get(fileId);
			obj.setData(data);
			obj.setKey(key);
			if(obj.getToRelease()){
				loS.release(fileId);
			}
			obj.setToRelease(false);
			objectsToSend.put(fileId, obj);
		}else{
			obj = new ObjectInQueue(fileId, data, key, false);
			boolean flag = false;
			if(activeObjects.containsValue(fileId)){
				Enumeration<Integer> e = activeObjects.keys();
				int elemKey;
				while(e.hasMoreElements()){
					elemKey = e.nextElement();
					if(activeObjects.get(elemKey).equals(fileId)){
						flag = sendingThreads.get(elemKey).setSameObjectToSend(obj);
					}
				}
			}			
			if(!flag){
				flag = false;
				for(int i = 0; i < sendingThreads.size(); i++){
					if(activeThreads.get(i)){
						activeThreads.put(i, false);
						sendingThreads.get(i).setObjectToSend(obj);
						activeObjects.put(i, fileId);
						i = sendingThreads.size() + 1;
						flag = true;
					}
				}
				if(!flag){
					objectsToSend.put(fileId, obj);
					queue.add(fileId);
				}
			}
		}	
	}

	public void removeSendingObject(String fileId){
		if(queue.contains(fileId)){
			queue.remove(fileId);
			loS.release(fileId);
			objectsToSend.remove(fileId);
		}
	}

	public boolean releaseSendingThread(int threadId){
		if(queue.size() > 0){
			String fileId = queue.poll();
			sendingThreads.get(threadId).setObjectToSend(objectsToSend.get(fileId));
			activeObjects.put(threadId, fileId);
			objectsToSend.remove(fileId);
			return true;
		}else{
			activeThreads.put(threadId, true);
			activeObjects.remove(threadId);
			return false;
		}
	}

	public void releaseSendigObject(String fileId, boolean toRelease){
		if(queue.contains(fileId)){
			ObjectInQueue obj = objectsToSend.get(fileId);
			if(obj.getToRelease()){
				loS.release(fileId);
			}else{
				obj.setToRelease(toRelease);
				objectsToSend.put(fileId, obj);
			}
		}else{
			boolean flag = false;
			for(int i = 0; i < sendingThreads.size(); i++){
				if(!activeThreads.get(i)){
					flag = sendingThreads.get(i).setToRelease(fileId, toRelease);
					if(flag)
						i=sendingThreads.size()+1;
				}
			}
			//o envio do objecto já terminou e o release nao foi feito
			if(!flag){
				loS.release(fileId);
			}
		}
	}

}
