package scfs.storageService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.crypto.SecretKey;

import scfs.directoryService.DirectoryService;
import scfs.directoryService.exceptions.DirectoryServiceException;
import scfs.general.Statistics;
import scfs.lockService.LockService;
import util.Pair;



public class StorageService implements IStorageService{

	private boolean useMemoryCache;
	private boolean assyncModelOn;

	private DataStatsManager dataManager;
	private DiskCacheManager diskManager;
	private MemoryCacheManager memoryManager;
	private SendingQueue queue;
	private IAccessor accessor;
	private DirectoryService diS;
	private LockService loS;

	public StorageService(int clientId, boolean useMemoryCache, boolean assyncModelOn, int numOfThreads, 
			int memorySize, DirectoryService diS, LockService loS){

		File cache = new File("cache_" + clientId);
		while(!cache.exists())
			cache.mkdir();
		this.diskManager = new DiskCacheManager("cache_" + clientId);

		List<String[][]> credentials = null;
		try {
			credentials = readCredentials();
		} catch (FileNotFoundException e) {
			System.out.println("accounts.properties file dosen't exist!");
			e.printStackTrace();
		} catch (ParseException e) {
			System.out.println("accounts.properties misconfigured!");		
			e.printStackTrace();
		}
		boolean isDepSkyAccessor = true;
		String accessKey = null;
		String secretKey = null;
		if(credentials.size() == 1){
			for(String[] pair : credentials.get(0)){
				if(pair[0].equalsIgnoreCase("accessKey")){
					accessKey = pair[1];
				}else if(pair[0].equalsIgnoreCase("secretKey")){
					secretKey = pair[1];
				}
			}

			this.accessor = new AmazonAccessor(clientId, accessKey, secretKey);
			isDepSkyAccessor = false;
		}else{
			this.accessor = new DepSkyAcessor(clientId);
		}

		this.diS = diS;
		this.loS = loS;
		this.dataManager = new DataStatsManager("cache_" + clientId);
		String[] fileNames = dataManager.recover();
		this.queue = new SendingQueue(dataManager, diS, loS, numOfThreads, clientId, isDepSkyAccessor, accessKey, secretKey);

		//to send all pending objects
		for(int i = 0; i < fileNames.length; i++){
			DataStats dt = dataManager.getDataStats(fileNames[i]);
			if(dt.getWriteInClouds()){
				byte[] data = diskManager.read(dt.getFileId(), 0, -1);
				if(data!=null)
					queue.addSendingObject(dt.getFileId(), data, null);
			}
		}

		if(useMemoryCache){
			this.memoryManager = new MemoryCacheManager(memorySize, diskManager);
			this.useMemoryCache = useMemoryCache;
		}
		if(assyncModelOn){
			this.assyncModelOn = assyncModelOn;
		}
	}

	public IAccessor getAccessor() {
		return accessor;
	}

	public int writeData(String fileId, byte[] value, int offset) {
		long time;
		DataStats dt = dataManager.getDataStats(fileId);
		if(dt == null){
			dt = dataManager.newDataStats(fileId);
		}
		if(!dt.getWriteInClouds())
			dataManager.setWriteInClouds(fileId, true, true);
		if(useMemoryCache){
			time = System.nanoTime();
			memoryManager.write(fileId, value, offset);
			Statistics.incWriteInMem(System.nanoTime() - time, value.length);
			if(!dt.getWriteInDisk()){
				dataManager.setWriteInDisk(fileId, true);
			}
		}else{
			time = System.nanoTime();
			diskManager.write(fileId, value, offset);
			Statistics.incWriteInDisk(System.nanoTime() - time, value.length);
		}
		if(offset + value.length > dt.getSize()){
			dataManager.setSize(fileId, offset + value.length, true);
		}

		return dataManager.getDataStats(fileId).getSize();
	}


	public byte[] readData(String fileId, int offset, int capacity,
			SecretKey key, byte[] hash, boolean isPending) {
		long time;
		byte[] data = null;
		if(updateCache(fileId, key, hash, isPending) == 0){
			if(useMemoryCache && memoryManager.isInCache(fileId)){
				time = System.nanoTime();
				data = memoryManager.read(fileId, offset, capacity);
				Statistics.incReadInMem(System.nanoTime()-time, data.length);
			}else{
				time = System.nanoTime();
				data = diskManager.read(fileId, offset, capacity);
				if(data!=null)
					Statistics.incReadInDisk(System.nanoTime()-time, data.length);
			}
		}
		return data;

	}


	public int truncData(String fileId, int size, SecretKey key, byte[] hash, boolean isToSyncWClouds, boolean isPending) {

		if(updateCache(fileId, key, hash, isPending) == -1)
			return -1;
		diskManager.truncate(fileId, size);
		if(useMemoryCache && memoryManager.isInCache(fileId)){
			memoryManager.truncate(fileId, size);
		}
		dataManager.setWriteInClouds(fileId, true, false);
		dataManager.setWriteInDisk(fileId, true);
		dataManager.setSize(fileId, size, true);
		if(isToSyncWClouds)
			syncWClouds(fileId, key);
		return 0;
	}


	public int deleteData(String fileId) {
		DataStats dt = dataManager.getDataStats(fileId);
		if(dt != null){
			if(useMemoryCache)
				memoryManager.delete(fileId);
			diskManager.delete(fileId);
			dataManager.deleteDataStats(fileId);
			if(assyncModelOn)
				queue.removeSendingObject(fileId);
		}
		return 0;
	}


	public byte[] syncWDisk(String fileId) {
		long time;
		DataStats dt = dataManager.getDataStats(fileId);
		if(dt != null){
			if(useMemoryCache && memoryManager.isInCache(fileId) && dt.getWriteInDisk()){
				time = System.nanoTime();	
				byte[] val = memoryManager.read(fileId, 0, dt.getSize());
				Statistics.incReadInMem(System.nanoTime()-time, val.length);
				time = System.nanoTime();				
				diskManager.write(fileId, val, 0);
				Statistics.incWriteInDisk(System.nanoTime()-time, val.length);
				if(!dt.getWriteInClouds())
					dataManager.setWriteInClouds(fileId, true, true);
				dataManager.setWriteInDisk(fileId, false);
				return val;
			}
		}
		return null;
	}


	public int syncWClouds(String fileId, SecretKey key) {
		long time;
		DataStats dt = dataManager.getDataStats(fileId);
		if(dt != null && dt.getWriteInClouds()){
			byte[] data = syncWDisk(fileId);
			if(data==null){
				time = System.nanoTime();
				data = diskManager.read(fileId, 0, dt.getSize());
				if(data==null)
					return -1;
				Statistics.incReadInDisk(System.nanoTime()-time, data.length);
			}
			dataManager.setWriteInClouds(fileId, false, false);
			if(assyncModelOn){
				queue.addSendingObject(fileId, data, key);
			}else{
				byte[] hash = accessor.writeTo(fileId, data, key);
				try {
					diS.commitMetadataBuffer(fileId, hash);
				} catch (DirectoryServiceException e) {
					e.printStackTrace();
				}
				dataManager.setHash(fileId, hash);
			}

			return 0;
		}
		return -1;
	}

	public int updateCache(String fileId, SecretKey key, byte[] hash, boolean isPending) {
		long time;
		DataStats dt = dataManager.getDataStats(fileId);
		if(dt == null){
			dt = dataManager.newDataStats(fileId);
		}
		if(!isPending){
			byte[] data = null;
			if(!diskManager.isInCache(fileId) || !Arrays.equals(dt.getHash(), hash)){
				data = accessor.readMatchingFrom(fileId, key, hash);
				if(data != null){
					time = System.nanoTime();
					diskManager.write(fileId, data, 0);
					Statistics.incWriteInDisk(System.nanoTime()-time, data.length);
					dataManager.setWriteInClouds(fileId, false, false);
					dataManager.setSize(fileId, data.length, false);
					dataManager.setHash(fileId, hash);
				}else
					return -1;
			}
			if(useMemoryCache){
				if(data != null){
					time = System.nanoTime();
					memoryManager.write(fileId, data, 0);
					Statistics.incWriteInMem(System.nanoTime()-time, data.length);
				}else if(!memoryManager.isInCache(fileId)){
					time = System.nanoTime();
					data = diskManager.read(fileId, 0, dt.getSize());

					if(data != null){
						Statistics.incReadInDisk(System.nanoTime()-time, data.length);
						time = System.nanoTime();
						memoryManager.write(fileId, data, 0);
						Statistics.incWriteInMem(System.nanoTime()-time, data.length);
					}else{
						return -1;
					}
				}
			}
		}
		return 0;
	}

	public int setPermition(String fileId, String permition, LinkedList<Pair<String,String>>cannonicalIds){
		return accessor.setPermition(fileId, permition, cannonicalIds);
	}


	public int releaseData(String fileId) {
		if(assyncModelOn){
			queue.releaseSendigObject(fileId, true);
		}else{
			loS.release(fileId);
		}
		return 0;
	}

	public int cleanMemory(String fileId){
		if(useMemoryCache && memoryManager.isInCache(fileId))
			memoryManager.delete(fileId);
		return 0;
	}

	/**
	 * Read the credentials of drivers accounts
	 */
	private List<String[][]> readCredentials() throws FileNotFoundException, ParseException{
		Scanner sc=new Scanner(new File("config"+File.separator+"accounts.properties"));
		//Scanner sc=new Scanner(new File("accounts.properties"));
		String line;
		String [] splitLine;
		LinkedList<String[][]> list = new LinkedList<String[][]>();
		int lineNum =-1;
		LinkedList<String[]> l2 = new LinkedList<String[]>();
		boolean firstTime = true;
		while(sc.hasNext()){
			lineNum++;
			line = sc.nextLine();
			if(line.startsWith("#") || line.equals(""))
				continue;
			else{
				splitLine = line.split("=", 2);
				if(splitLine.length!=2){
					sc.close();
					throw new ParseException("Bad formated accounts.properties file.", lineNum);
				}else{
					if(splitLine[0].equals("driver.type")){
						if(!firstTime){
							String[][] array= new String[l2.size()][2];
							for(int i = 0;i<array.length;i++)
								array[i] = l2.get(i);
							list.add(array);
							l2 = new LinkedList<String[]>();
						}else
							firstTime = false;
					}
					l2.add(splitLine);
				}
			}
		}
		String[][] array= new String[l2.size()][2];
		for(int i = 0;i<array.length;i++)
			array[i] = l2.get(i);
		list.add(array);
		sc.close();
		return list;
	}

}
