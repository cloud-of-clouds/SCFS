package scfs.storageService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;


public class DataStatsManager {

	private ConcurrentHashMap<String, DataStats> dataStats;
	private String infoDir;
	private String cache;


	public DataStatsManager(String cache){
		
		this.dataStats = new ConcurrentHashMap<String, DataStats>();
		this.cache = cache;
		this.infoDir = this.cache + File.separator + "info directory" + File.separator;
		File f = new File(infoDir);
		if(!f.exists()){
			System.out.println(f.getPath());
			while(!f.mkdir());
		}
	}

	public String[] recover(){

		File file = new File(infoDir);
		String[] infoFiles = null;
		if(file.isDirectory()){
			infoFiles = file.list();
			DataStats dt = null;
			for(int i = 0; i < infoFiles.length; i++){
				dt = readInfoFromDisk(infoFiles[i]);
				dataStats.put(infoFiles[i], dt);
			}
		}
		return infoFiles;
	}

	public DataStats getDataStats(String fileId){
		return dataStats.get(fileId);
	}

	public DataStats newDataStats(String fileId){
		DataStats dt = new DataStats(fileId);
		dataStats.put(fileId, dt);
		return dt;
	}

	public void setHash(String fileId, byte[] hash){
		if(dataStats.containsKey(fileId)){
			DataStats dt = dataStats.get(fileId);
			dt.setHash(hash);
			writeInfoToDisk(dt);
		}else{ 	}
	}

	public void setWriteInClouds(String fileId, boolean writeInclouds, boolean isToSaveInDisk){
		if(dataStats.containsKey(fileId)){
			DataStats dt = dataStats.get(fileId);
			dt.setWriteInClouds(writeInclouds);
			if(isToSaveInDisk)
				writeInfoToDisk(dt);
		}else{	}
	}

	public void setWriteInDisk(String fileId, boolean writeInDisk){
		DataStats dt = dataStats.get(fileId);
		dt.setWriteInDisk(writeInDisk);
	}

	public void setSize(String fileId, int size, boolean isToSaveInDisk){
		DataStats dt = dataStats.get(fileId);
		dt.setSize(size);
		if(isToSaveInDisk)
			writeInfoToDisk(dt);
	}

	public void deleteDataStats(String fileId){
		File file = new File(infoDir+fileId);
		if(file.exists()){
			file.delete();
		}
		dataStats.remove(fileId);
		//dizer que o ficheiro foi elimindado ao garbage collector
	}

	public void setNewVersionTranfered(String fileId, int numBytes){

	}

	private void writeInfoToDisk(DataStats dataStats){
		File rootCache = new File(cache);
		while(!rootCache.exists())
			rootCache.mkdir();
		File info = new File(infoDir);
		while(!info.exists())
			info.mkdir();
		try {
			FileOutputStream fout = new FileOutputStream(infoDir+dataStats.getFileId());
			ObjectOutputStream out = new ObjectOutputStream(fout);
			out.writeObject(dataStats);
			out.flush();
			out.close();
			fout.flush();
			fout.close();			

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private DataStats readInfoFromDisk(String fileId){
		File rootCache = new File(cache);
		while(!rootCache.exists())
			rootCache.mkdir();
		File info = new File(infoDir);
		while(!info.exists())
			info.mkdir();
		try{
			FileInputStream fin = new FileInputStream(infoDir+fileId);
			ObjectInputStream in = new ObjectInputStream(fin);
			DataStats dt = (DataStats) in.readObject();
			in.close();
			fin.close();
			return dt;
		}catch (IOException e) {
			e.printStackTrace();
		}catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
