package scfs.general;

import fuse.FuseFtype;
import general.DepSpaceException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.crypto.SecretKey;

import scfs.cache.MetadataCacheOnSyncDirectoryService;
import scfs.directoryService.FileStats;
import scfs.directoryService.NoCacheDirectoryService;
import scfs.directoryService.NodeMetadata;
import scfs.directoryService.exceptions.DirectoryServiceException;
import client.DepSpaceAccessor;
import client.DepSpaceAdmin;
import depskys.core.MyAESCipher;

public class DiSMicroBench {

	private static final int NUM_IT = 450;
	private static final int NUM_START = 150;
	private static final int NUM_FILES_DIR = 50;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		int clientId = Integer.parseInt(args[0]);
		MetadataCacheOnSyncDirectoryService dis = null;
		ArrayList<String> list = new ArrayList<String>(NUM_IT);
		ArrayList<NodeMetadata> mList = new ArrayList<NodeMetadata>(NUM_IT);
		int[] put = new int[NUM_IT-NUM_START];
		int[] get = new int[NUM_IT-NUM_START];
		int[] update = new int[NUM_IT-NUM_START];
		int[] del = new int[NUM_IT-NUM_START];
		int[] getdir = new int[NUM_IT-NUM_START];

		SecretKey defaultKey = null;
		try {
			defaultKey = MyAESCipher.generateSecretKey();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		try {
			dis = new MetadataCacheOnSyncDirectoryService(new NoCacheDirectoryService(clientId, init("microBench", false, clientId)));
		} catch (DepSpaceException e) {
			e.printStackTrace();
			System.exit(0);
		}

		System.out.println("Will Start Now.");
		long start;
		System.out.println("\t0 - ALLOCATING RESOURCES.");
		start = System.currentTimeMillis();
		NodeMetadata m = new NodeMetadata(NodeType.DIR, "/", "DIR" , createDefaultFileStats(NodeType.DIR, start, 0), ""+clientId+start, defaultKey, new int[] {clientId}, new int[] {clientId});
		try {
			dis.putMetadata( m );
		} catch (DirectoryServiceException e) {
			e.printStackTrace();
		}

		for(int i=0; i<NUM_FILES_DIR ; i++){
			m = new NodeMetadata(NodeType.FILE, "/DIR", "000000000000000000000000000000" + i, createDefaultFileStats(NodeType.FILE, start, 0), ""+clientId+start, defaultKey, new int[] {clientId}, new int[] {clientId});
		}
		//PUT
		System.out.println("\t1 - PUT FILES.");
		for(int i = 0 ; i<NUM_IT ; i++) {
			String parent = "/";
			list.add(parent.concat("/000000000000000000000000000000" + i));

			start = System.currentTimeMillis();
			m = new NodeMetadata(NodeType.FILE, parent, "000000000000000000000000000000" + i, createDefaultFileStats(NodeType.FILE, start, 0), ""+clientId+start, defaultKey, new int[] {clientId}, new int[] {clientId});
			mList.add(m); 
			try {
				start = System.currentTimeMillis();
				dis.putMetadata( m );
				if(i>=NUM_START)
					put[i-NUM_START] = (int)(System.currentTimeMillis()-start);
			} catch (DirectoryServiceException e) {
				e.printStackTrace();
			}
		}

		//UPDATE
		System.out.println("\t2 - UPDATE FILES.");
		for(int i = 0 ; i<NUM_IT ; i++) {
			try {
				FileStats fs = mList.get(i).getStats();
				fs.setSize(fs.getSize()+10);
				mList.get(i).setStats(fs);

				start = System.currentTimeMillis();
				dis.updateMetadata(list.get(i), mList.get(i));
				if(i>=NUM_START)
					update[i-NUM_START] = (int)(System.currentTimeMillis()-start);
			} catch (DirectoryServiceException e) {
				e.printStackTrace();
			}
		}

		//GET
		System.out.println("\t3 - GET FILES.");
		for(int i = 0 ; i<NUM_IT ; i++) {
			try {
				start = System.currentTimeMillis();
				dis.getMetadata(list.get(i));
				if(i>=NUM_START)
					get[i-NUM_START] = (int)(System.currentTimeMillis()-start);
			} catch (DirectoryServiceException e) {
				e.printStackTrace();
			}
		}

		//GET_DIR
		System.out.println("\t4 - DET DIR.");
		for(int i = 0 ; i<NUM_IT ; i++) {
			try {
				start = System.currentTimeMillis();
				dis.getNodeChildren("/DIR");
				if(i>=NUM_START)
					getdir[i-NUM_START] = (int)(System.currentTimeMillis()-start);
			} catch (DirectoryServiceException e) {
				e.printStackTrace();
			}
		}

		try {
			for(int i = 0; i<NUM_FILES_DIR ; i++){
				dis.removeMetadata("/DIR/000000000000000000000000000000"+i);
			}
			dis.removeMetadata("/DIR");
		} catch (DirectoryServiceException e) {
			e.printStackTrace();
		}
		
		
		//DEL
		System.out.println("\t5 - DELETE FILES.");
		for(int i = 0 ; i<NUM_IT ; i++) {
			try {

				start = System.currentTimeMillis();
				dis.removeMetadata(list.get(i));
				if(i>=NUM_START)
					del[i-NUM_START] = (int)(System.currentTimeMillis()-start);
			} catch (DirectoryServiceException e) {
				e.printStackTrace();
			}
		}

		System.out.println("\t6 - GENERATING RESULTS.");
		File f = new File("MicroBench");
		if(!f.exists())
			f.mkdir();

		f = new File("MicroBench/putMicroBench.txt");
		if(!f.exists())
			f.createNewFile();
		FileWriter fw = new FileWriter( f );
		fw.append("Latencies:\n\n");
		double med = med(put);
		for(int time : put)
			fw.append("\t"+time+" ms\n");
		fw.append("\nMed: " + med + " ms");
		fw.append("\nDP: " + dp(put, med));
		fw.close();


		f = new File("MicroBench/updateMicroBench.txt");
		if(!f.exists())
			f.createNewFile();
		fw = new FileWriter( f );
		fw.append("Latencies:\n\n");
		med = med(update);
		for(int time : update)
			fw.append("\t"+time+" ms\n");
		fw.append("\nMed: " + med + " ms");
		fw.append("\nDP: " + dp(update, med));
		fw.close();


		f = new File("MicroBench/getMicroBench.txt");
		if(!f.exists())
			f.createNewFile();
		fw = new FileWriter( f );
		fw.append("Latencies:\n\n");
		med = med(get);
		for(int time : get)
			fw.append("\t"+time+" ms\n");
		fw.append("\nMed: " + med + " ms");
		fw.append("\nDP: " + dp(get, med));
		fw.close();


		f = new File("MicroBench/getDirMicroBench.txt");
		if(!f.exists())
			f.createNewFile();
		fw = new FileWriter( f );
		fw.append("Latencies:\n\n");
		med = med(getdir);
		for(int time : getdir)
			fw.append("\t"+time+" ms\n");
		fw.append("\nMed: " + med + " ms");
		fw.append("\nDP: " + dp(getdir, med));
		fw.close();

		f = new File("MicroBench/delMicroBench.txt");
		if(!f.exists())
			f.createNewFile();
		fw = new FileWriter( f );
		fw.append("Latencies:\n\n");
		med = med(del);
		for(int time : del)
			fw.append("\t"+time+" ms\n");
		fw.append("\nMed: " + med + " ms");
		fw.append("\nDP: " + dp(del, med));
		fw.close();

		System.out.println("Done!");
		System.exit(0);


	}

	private static double med(int[] array){
		int sum = 0 ;
		for( int i : array)
			sum+=i;

		return (double)((double)sum/(double)array.length);
	}


	private static double dp(int[] array, double mediaAritimetica) {  
		if (array.length == 1) {  
			return 0.0;  
		} else {  
			double somatorio = 0l;  
			for (int i = 0; i < array.length; i++) {  
				double result = array[i] - mediaAritimetica;  
				somatorio = somatorio + result * result;  
			}  
			return Math.sqrt(((double) 1 /( array.length-1))  
					* somatorio);  
		}  
	} 


	private static DepSpaceAccessor init(String tsName, boolean confidencial, int clientId) {
		Properties prop = new Properties();
		// the DepSpace name
		prop.put(util.TSUtil.DPS_NAME, tsName);
		// use confidentiality?
		prop.put(util.TSUtil.DPS_CONFIDEALITY, Boolean.toString(confidencial));

		// the DepSpace Accessor, who will access the DepSpace.
		DepSpaceAccessor accessor = null;

		try {
			accessor = new DepSpaceAdmin(clientId).createSpace(prop);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (DepSpaceException e) {
			try {
				accessor = new DepSpaceAdmin(clientId).createAccessor(prop);
			} catch (Exception e1) {
				return null;
			}
		}

		return accessor;
	}

	public static FileStats createDefaultFileStats(NodeType nodeType, long inode, int mode) {
		if(nodeType == NodeType.SYMLINK)
			mode = FuseFtype.TYPE_SYMLINK | 0777;
		else if(nodeType == NodeType.DIR)
			mode = FuseFtype.TYPE_DIR | 0755;
		else
			mode = FuseFtype.TYPE_FILE | 0644;

		int nlink = 1;
		int uid = 0; 
		int gid = 0;
		int rdev = 0;
		int size = 0;
		long blocks = 0;
		int currentTime = (int) (System.currentTimeMillis() / 1000L);
		int atime = currentTime, mtime = currentTime, ctime = currentTime;
		return new FileStats(inode, mode, nlink, uid, gid, rdev, size, blocks, atime, mtime, ctime, new byte[20], nodeType == NodeType.FILE ? true : false, true);
	}

}
