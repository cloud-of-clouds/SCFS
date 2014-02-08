package scfs.general;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.crypto.SecretKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import scfs.cache.MetadataCacheOnSyncDirectoryService;
import scfs.directoryService.DirectoryService;
import scfs.directoryService.DirectoryServiceRedirect;
import scfs.directoryService.FileStats;
import scfs.directoryService.NoCacheDirectoryService;
import scfs.directoryService.NoSharingDirectoryService;
import scfs.directoryService.NodeMetadata;
import scfs.directoryService.PrivateNameSpaceStats;
import scfs.directoryService.exceptions.DirectoryServiceConnectionProblemException;
import scfs.directoryService.exceptions.DirectoryServiceException;
import scfs.lockService.LockService;
import scfs.storageService.StorageService;
import util.Pair;
import client.DepSpaceAccessor;
import client.DepSpaceAdmin;
import depskys.core.MyAESCipher;
import fuse.Filesystem3;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;
import fuse.FuseMount;
import fuse.FuseOpenSetter;
import fuse.FuseSizeSetter;
import fuse.FuseStatfs;
import fuse.FuseStatfsSetter;
import fuse.XattrLister;
import fuse.XattrSupport;
import general.DepSpaceException;

/**
 * Shared cloud-backed file system implementation.
 * 
 * @author rmendes (rmendes@lasige.di.fc.ul.pt)
 * @author toliveira (toliveira@lasige.di.fc.ul.pt)
 * 
 */
public class SCFS implements Filesystem3, XattrSupport {

	private static final int LOCK_TIME = 400000; 
	private int clientId;
	private FuseStatfs statfs;
	private DepSpaceAccessor accessor;
	private StorageService daS;
	private DirectoryService directoryService;
	private LockService lockService;
	private NoSharingDirectoryService namespace;

	private Map<String, Boolean> lockedFiles;
	private SecretKey defaultKey;
	private static byte[] emptyHash;
	private PrivateNameSpaceStats namespaceStats;
	private Configure config;
	private FileStats statistics;
	private int fileNum;
	private int [] numOp;
	private String[] ops;

	public SCFS(Configure config) throws DepSpaceException {
		LinkedList<String[]> l = new LinkedList<String[]>();
		try {
			List<String[][]> list = readCredentials();
			String[] vec = new String[2];

			for(String[][] str : list){
				for(String[] v : str){
					if(v[0].equals("driver.type"))
						vec[0]=v[1];
					else if(v[0].equals("canonicalId"))
						vec[1]=v[1];
				}
				l.add(vec);
				vec = new String[2]; 
			}
		} catch (FileNotFoundException e) {
			System.err.println("Accounts credential file not found (accounts.properties)");
			System.exit(1);
		} catch (ParseException e) {
			System.err.println(e.getMessage() + " [ line : "+e.getErrorOffset()+" ]");
			System.exit(1);
		}

		ops = new String[]{"chmod", "chown" , "flush", "fsync", "getattr" , "getdir", "link", "mkdir", "mknod", "open", "read", "readlink", "release", "rename", "rmdir", "statfs", "symlink", "truncate", "truncate", "unlink", "utime", "write"};
		numOp = new int[ops.length];
		for(int i=0;i<numOp.length;i++)
			numOp[i]=0;


		this.config = config;
		System.out.println("Debug = " + config.getIsPrinter());
		System.out.println("Use memory cache = " + config.getIsOptimizedCache());
		System.out.println("Assync model = " + config.getIsSync());
		System.out.println("Is non-blocking to cloud = " + config.getIsNomBlockCloud());
		System.out.println("NonSharing = " + config.isNonSharing());
		Statistics.reset();

		MetadataCacheOnSyncDirectoryService.DELTA_TIME = this.config.getDelta();
		this.clientId = config.getClientId();

		String disId = "NS" + clientId;
		try {
			FileInputStream fis = new FileInputStream("config" + File.separator + "bucket_name.properties");
			Properties props = new Properties();  
			props.load(fis);  
			fis.close();  
			String name = props.getProperty("ns");
			if(name.length() == 0){
				char[] randname = new char[10];
				for(int i = 0; i < 10; i++){
					char rand = (char)(Math.random() * 26 + 'a');
					randname[i] = rand;
				}
				disId = new String(randname);
				props.setProperty("ns", disId);
				props.store(new FileOutputStream("config" + File.separator + "bucket_name.properties") ,"change");
			}else
				disId = name;
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.lockedFiles = new HashMap<String, Boolean>();
		Printer.setPrintAuth(config.getIsPrinter());
		try {
			this.defaultKey = MyAESCipher.generateSecretKey();
		} catch (Exception e) { e.printStackTrace(); }


		NoCacheDirectoryService noCacheDis = null;
		if(!config.isNonSharing()){
			this.accessor = init("SCFS", false, clientId);
			noCacheDis = new NoCacheDirectoryService(clientId, accessor);
			this.directoryService = new MetadataCacheOnSyncDirectoryService(noCacheDis);
		}else{
			SecretKey k = null;
			this.directoryService = new NoSharingDirectoryService(disId, k, false);
		}

		//init PNS
		if(config.isNonSharing()){
			namespace = new NoSharingDirectoryService("", null, true);
		}else{
			try {
				namespaceStats = directoryService.getPrivateNameSpaceMetadata();
			} catch (DirectoryServiceException e) {
				try {
					String pathId = getNextIdPath();
					SecretKey key = MyAESCipher.generateSecretKey();
					namespaceStats = new PrivateNameSpaceStats(pathId, key);
					directoryService.putPrivateNameSpaceMetadata(clientId, namespaceStats);

				} catch (DirectoryServiceException e1) {
					e1.printStackTrace();
					if(e1 instanceof DirectoryServiceConnectionProblemException)
						System.out.println("Cannot create the private namespace metadata");
				} catch (Exception e1) {		e1.printStackTrace(); }
			}
			namespace = new NoSharingDirectoryService(namespaceStats.getIdPath(), namespaceStats.getKey(), true);
		}

		DirectoryService redirect = new DirectoryServiceRedirect(clientId, directoryService, namespace);

		this.lockService = new LockService(accessor, clientId);
		this.daS = new StorageService(clientId, config.getIsOptimizedCache(), config.getIsNomBlockCloud(), 4, 0, redirect, lockService);
		if(config.isNonSharing()){
			try {
				((NoSharingDirectoryService)directoryService).setStorageService(daS);
			} catch (IOException e) {
				//Wrong PASSWORD
				e.printStackTrace();
				System.out.println("Wrong Password.");
				daS.deleteData(disId);
				System.exit(0);
			}
		}else{
			try {
				namespace.setStorageService(daS);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Personal Namespace corrupted.");
			}
		}

		try {
			directoryService.putCredentials(l);
		} catch (DirectoryServiceConnectionProblemException e) {
			System.out.println("Cannot put client credentials");
		}

		statistics = createDefaultFileStats(NodeType.FILE, Long.parseLong(getNextIdPath()), 0755);

		fileNum=0;

		statfs = new FuseStatfs();
		statfs.blocks = 0;
		statfs.blockSize = SCFSConstants.BLOCK_SIZE;
		statfs.blocksFree = 73400320;
		statfs.files = 0;
		statfs.filesFree = 0;
		statfs.namelen = 2048;
		emptyHash = new byte[0];

		System.out.println("DELTA = " + MetadataCacheOnSyncDirectoryService.DELTA_TIME);

		System.out.println("C2FS mounted.");
	}

	@Override
	public int getattr(String path, FuseGetattrSetter getattrSetter) throws FuseException {
		Printer.println("\n :::: GETATTR( " + path + " )", "amarelo");

		numOp[4]++;

		if(path.equals("/.statistics.txt")){
			getattrSetter.set(statistics.getInode(), statistics.getMode(), statistics.getNlink(), statistics.getUid(), statistics.getGid(),
					statistics.getRdev(), Statistics.getReport().length() + getOpsString().length(), statistics.getBlocks(), statistics.getAtime(),
					statistics.getMtime(), statistics.getCtime());
			return 0;
		}

		NodeMetadata metadata = null;
		if(!config.isNonSharing()){
			try {
				metadata = namespace.getMetadata(path);

			} catch (DirectoryServiceConnectionProblemException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			} catch (DirectoryServiceException e) {
				long time = System.currentTimeMillis();
				try {
					metadata = directoryService.getMetadata(path);
				} catch (DirectoryServiceException e1) {
					throw new FuseException(e1.getMessage()).initErrno(FuseException.ENOENT);
				}
				Statistics.incGetMeta(System.currentTimeMillis()-time);
			} 
		}else{
			long time = System.currentTimeMillis();
			try {
				metadata = directoryService.getMetadata(path);
			} catch (DirectoryServiceException e1) {
				throw new FuseException(e1.getMessage()).initErrno(FuseException.ENOENT);
			}
			Statistics.incGetMeta(System.currentTimeMillis()-time);
		}
		FileStats stats = metadata.getStats();
		getattrSetter.set(stats.getInode(), stats.getMode(), stats.getNlink(), Integer.parseInt(System.getProperty("uid")), Integer.parseInt(System.getProperty("gid")),
				stats.getRdev(), stats.getSize(), stats.getBlocks(), stats.getAtime(),
				stats.getMtime(), stats.getCtime());

		return 0;
	}

	@Override
	public int getdir(String path, FuseDirFiller dirFiller) throws FuseException {
		Printer.println("\n :::: GETDIR( " + path + " )", "amarelo");

		numOp[5]++;

		if(!config.isNonSharing()){
			boolean exists=true;
			try {
				long time = System.currentTimeMillis();
				Collection<NodeMetadata> nodes = directoryService.getNodeChildren(path);
				Statistics.incGetDirMeta(System.currentTimeMillis() - time);
				for (NodeMetadata n : nodes) {
					dirFiller.add(n.getName(), n.getStats().getInode(), n.getStats().getMode());
				}
			} catch (DirectoryServiceConnectionProblemException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			} catch (DirectoryServiceException e) {
				exists=false;
			} 
			try {
				for(NodeMetadata n : namespace.getNodeChildren(path)){
					dirFiller.add(n.getName(), n.getStats().getInode(), n.getStats().getMode());
				}
			} catch (DirectoryServiceException e1) {
				if(!exists)
					throw new FuseException(e1.getMessage()).initErrno(FuseException.ENOTDIR);
			}
		}else{
			try {
				long time = System.currentTimeMillis();
				Collection<NodeMetadata> nodes = directoryService.getNodeChildren(path);
				Statistics.incGetDirMeta(System.currentTimeMillis() - time);
				for (NodeMetadata n : nodes) {
					dirFiller.add(n.getName(), n.getStats().getInode(), n.getStats().getMode());
				}
			} catch (DirectoryServiceConnectionProblemException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			} catch (DirectoryServiceException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ENOTDIR);
			} 
		}

		if(path.equals("/"))
			dirFiller.add(".statistics.txt", statistics.getInode(), statistics.getMode());
		return 0;
	}

	@Override
	public int mkdir(String path, int mode) throws FuseException {
		Printer.println("\n :::: MKDIR( " + path + " )", "amarelo");

		numOp[7]++;

		try {

			String[] vec = dividePath(path);
			String idPath = getNextIdPath();
			FileStats fs = createDefaultFileStats(NodeType.DIR, Long.parseLong(idPath), mode);
			NodeMetadata m = new NodeMetadata(NodeType.DIR, vec[0], vec[1], fs, idPath, defaultKey, new int[]{clientId}, new int[]{clientId});

			long time = System.currentTimeMillis();
			directoryService.putMetadata(m);
			Statistics.incPutMeta(System.currentTimeMillis()-time);
			//			}
		} catch (DirectoryServiceConnectionProblemException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		} catch (DirectoryServiceException e) {
			throw new FuseException("Directory already exists").initErrno(FuseException.EALREADY);
		} 

		return 0;
	}

	@Override
	public int mknod(String path, int mode, int rdev) throws FuseException {
		Printer.println("\n :::: MKNOD( " + path + ", "+ mode + " )", "amarelo");

		numOp[8]++;

		try {
			String[] vec = dividePath(path);
			String idPath = getNextIdPath();

			FileStats fs = createDefaultFileStats(NodeType.FILE, Long.parseLong(idPath), mode);
			fs.setMode(mode);
			fs.setRdev(rdev);
			NodeMetadata m = new NodeMetadata(NodeType.FILE, vec[0], vec[1], fs, idPath, MyAESCipher.generateSecretKey(), new int[]{clientId}, new int[]{clientId});

			long time = System.currentTimeMillis();
			directoryService.putMetadata(m);
			Statistics.incPutMeta(System.currentTimeMillis()-time);

			statfs.blocks = statfs.blocks + (SCFSConstants.BLOCK_SIZE - 1) / SCFSConstants.BLOCK_SIZE;
		} catch (DirectoryServiceConnectionProblemException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		} catch (DirectoryServiceException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		}  catch (Exception e) {
			throw new FuseException("Impossible to create a secure file").initErrno(FuseException.ECONNABORTED);
		}

		return 0;
	}

	@Override
	public int open(String path, int flags, FuseOpenSetter openSetter) throws FuseException {
		Printer.println("\n :::: OPEN( "	+ path	+ ", " + (((flags & SCFSConstants.O_ACCMODE) == O_RDWR) ? "read_write" : ((flags & SCFSConstants.O_ACCMODE) == O_WRONLY) ? "write" : "read") + " )", "amarelo");

		numOp[9]++;

		if(path.equals("/.statistics.txt"))
			return 0;
		NodeMetadata nm = null;
		if(!config.isNonSharing()){

			nm = this.getMetadata(path);

			if ((flags & SCFSConstants.O_ACCMODE) == O_WRONLY || (flags & SCFSConstants.O_ACCMODE) == O_RDWR && !nm.getStats().isPrivate()) {	
				if(lockService.tryAcquire(nm.getId_path(), LOCK_TIME)){
					lockedFiles.put(nm.getId_path(), true);
				}else{
					throw new FuseException("No Lock available.").initErrno(FuseException.ENOLCK);
				}
			}
		}else {
			try {
				long time = System.currentTimeMillis();
				nm = directoryService.getMetadata(path);
				Statistics.incGetMeta(System.currentTimeMillis() - time);
			} catch (DirectoryServiceException e1) {
				throw new FuseException(e1.getMessage()).initErrno(FuseException.ENOENT);
			}
		}


		daS.updateCache(nm.getId_path(), nm.getKey(), nm.getStats().getDataHash(), nm.getStats().isPending());
		openSetter.setFh(nm);

		return 0;
	}

	private NodeMetadata getMetadata(String path) throws FuseException{
		boolean inPNS = true;
		NodeMetadata nm = null;
		try{
			nm = namespace.getMetadata(path);

		} catch (DirectoryServiceConnectionProblemException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		}catch (DirectoryServiceException e) {
			try {
				long time = System.currentTimeMillis();
				nm = directoryService.getMetadata(path);
				inPNS=false;
				Statistics.incGetMeta(System.currentTimeMillis() - time);
			} catch (DirectoryServiceException e1) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ENOENT);
			}
		}
		nm.getStats().setPrivate(inPNS);
		return nm;

	}

	@Override
	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
		Printer.println("\n :::: READ( " + path + ", offset:" + offset + " )", "amarelo");

		numOp[10]++;

		NodeMetadata metadata = this.getMetadata(path);
		if (path.equals("/.statistics.txt") || !metadata.isDirectory() && !metadata.getStats().isPending()) {

			byte[] value = null;

			if(path.equals("/.statistics.txt")){
				value = Statistics.getReport().concat("\n").concat(getOpsString()).getBytes();
				for(int i=0;i<numOp.length;i++){
					numOp[i]=0;
				}
				Statistics.reset();
			}else
				value = daS.readData(metadata.getId_path(), (int)offset, buf.capacity(), metadata.getKey(), metadata.getStats().getDataHash(), metadata.getStats().isPending());

			if(value == null)
				throw new FuseException("Cannot read.").initErrno(FuseException.EIO);
			try{
				buf.put(value);
			}catch (Exception e) {
			}
		}

		return 0;
	}

	@Override
	public int write(String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {

		numOp[20]++;

		Printer.println("\n :::: WRITE( " + path + ", isWritepage: " + isWritepage + ", offset: " + offset + " )", "amarelo");

		if(path.equals("/.statistics.txt"))
			return 0;

		try {
			NodeMetadata metadata = this.getMetadata(path);
			byte[] a = new byte[buf.capacity()];
			int i = 0;
			while (buf.hasRemaining()) {
				a[i] = buf.get();
				i++;
			}
			int size = daS.writeData(metadata.getId_path(), a, (int)offset);
			if(size==-1)
				throw new FuseException("IOException on write.").initErrno(FuseException.ECONNABORTED);

			NodeMetadata meta = null;
			try {
				meta = (NodeMetadata) metadata.clone();
			} catch (CloneNotSupportedException e) { e.printStackTrace();	}
			meta.getStats().setSize(size);
			meta.getStats().setPending(false);
			metadata.getStats().setSize(size);
			metadata.getStats().setPending(false);
			if(!config.isNonSharing() && namespace.containsMetadata(path))
				namespace.insertMetadataInBuffer(metadata.getId_path(), meta);
			else
				directoryService.insertMetadataInBuffer(metadata.getId_path(), meta);

		} catch (DirectoryServiceConnectionProblemException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		} catch (DirectoryServiceException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		}
		return 0;
	}

	@Override
	public int rename(String from, String to) throws FuseException {

		numOp[13]++;

		Printer.println("\n :::: RENAME( from: " + from + ", to: " + to + " )", "amarelo");
		if(from.equals("/.statistics.txt"))
			return 0;

		String[] vec = dividePath(to);
		NodeMetadata metadata=null;
		if(!config.isNonSharing()){
			boolean isInPNS;
			try {
				metadata = namespace.getMetadata(from);
				isInPNS = true;
			} catch (DirectoryServiceException e) {
				try {
					long time = System.currentTimeMillis();
					metadata = directoryService.getMetadata(from);
					Statistics.incGetMeta(System.currentTimeMillis() - time);
				} catch (DirectoryServiceException e1) {
					throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
				}
				isInPNS = false;
			}


			try{
				if(isInPNS){
					namespace.updateMetadata(from, new NodeMetadata(metadata.getNodeType(), vec[0], vec[1], metadata.getStats(), metadata.getId_path(), metadata.getKey(), metadata.getC_r(), metadata.getC_w()));
				}else{
					NodeMetadata node=null;

					node = new NodeMetadata(metadata.getNodeType(), vec[0], vec[1], metadata.getStats(), metadata.getId_path(), metadata.getKey(), metadata.getC_r(), metadata.getC_w());

					long time = System.currentTimeMillis();
					directoryService.updateMetadata(from, node);
					Statistics.incUpdateMeta(System.currentTimeMillis() - time);

				}
			} catch (DirectoryServiceConnectionProblemException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			} catch (DirectoryServiceException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			} 
		}else{
			try {
				long time = System.currentTimeMillis();
				metadata = directoryService.getMetadata(from);
				Statistics.incGetMeta(System.currentTimeMillis() - time);

				NodeMetadata node=new NodeMetadata(metadata.getNodeType(), vec[0], vec[1], metadata.getStats(), metadata.getId_path(), metadata.getKey(), metadata.getC_r(), metadata.getC_w());

				time = System.currentTimeMillis();
				directoryService.updateMetadata(from, node);
				Statistics.incUpdateMeta(System.currentTimeMillis() - time);
			} catch (DirectoryServiceException e1) {
				throw new FuseException(e1.getMessage()).initErrno(FuseException.ECONNABORTED);
			}
		}

		return 0;
	}

	@Override
	public int chmod(String path, int mode) throws FuseException {
		Printer.println("\n :::: CHMOD( " + path + ", mode: " + mode + " )", "amarelo");
		if(path.equals("/.statistics.txt"))
			return 0;

		numOp[0]++;

		NodeMetadata metadata=null;
		if(!config.isNonSharing()){
			boolean isInPNS;
			try {
				metadata = namespace.getMetadata(path);
				isInPNS = true;
			} catch (DirectoryServiceException e1) {
				try {
					long time = System.currentTimeMillis();
					metadata = directoryService.getMetadata(path);
					Statistics.incGetMeta(System.currentTimeMillis() - time);
				} catch (DirectoryServiceException e) {
					throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
				}
				isInPNS = false;
			}

			if (mode == metadata.getStats().getMode())
				return 0;

			try {
				FileStats fs = metadata.getStats();
				fs.setMode(mode);
				NodeMetadata node = new NodeMetadata(metadata.getNodeType(), metadata.getParent(), metadata.getName(), fs, metadata.getId_path(), metadata.getKey(), ((mode & SCFSConstants.S_IRGRP) == 0 && (mode & SCFSConstants.S_IROTH) == 0) ? new int[] { clientId } : null, metadata.getC_w());

				if(isInPNS){
					namespace.updateMetadata(path, node);
				}else{
					long time = System.currentTimeMillis();
					directoryService.updateMetadata(path, node);
					Statistics.incUpdateMeta(System.currentTimeMillis() - time);
				}
			} catch (DirectoryServiceConnectionProblemException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			} catch (DirectoryServiceException e) {
				e.printStackTrace();
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			}
		}else{
			try {
				long time = System.currentTimeMillis();
				metadata = directoryService.getMetadata(path);
				Statistics.incGetMeta(System.currentTimeMillis() - time);

				if (mode == metadata.getStats().getMode())
					return 0;

				FileStats fs = metadata.getStats();
				fs.setMode(mode);
				NodeMetadata node = new NodeMetadata(metadata.getNodeType(), metadata.getParent(), metadata.getName(), fs, metadata.getId_path(), metadata.getKey(), ((mode & SCFSConstants.S_IRGRP) == 0 && (mode & SCFSConstants.S_IROTH) == 0) ? new int[] { clientId } : null, metadata.getC_w());

				time = System.currentTimeMillis();
				directoryService.updateMetadata(path, node);
				Statistics.incUpdateMeta(System.currentTimeMillis() - time);

			} catch (DirectoryServiceException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			}
		}
		return 0;
	}

	@Override
	public int flush(String path, Object fh) throws FuseException {
		Printer.println("\n :::: FLUSH( " + path + " )", "amarelo");
		if(path.equals("/.statistics.txt"))
			return 0;

		numOp[2]++;
		NodeMetadata mdata = (NodeMetadata) fh;
		if(mdata != null && !mdata.getStats().isPending()){
			daS.syncWClouds(mdata.getId_path(), mdata.getKey());
		}

		return 0;
	}

	@Override
	public int fsync(String path, Object fh, boolean isDatasync) throws FuseException {
		Printer.println("\n :::: FSYNC(" + path + ", isDatasync: " + isDatasync + " )", "amarelo");
		if(path.equals("/.statistics.txt"))
			return 0;

		numOp[3]++;

		NodeMetadata nm = (NodeMetadata) fh;
		if(!config.getIsSync()){
			daS.syncWDisk(nm.getId_path());
		}else{
			daS.syncWClouds(nm.getId_path(), nm.getKey());
		}

		return 0;
	}

	@Override
	public int link(String from, String to) throws FuseException {
		Printer.println("\n :::: LINK(from: " + from + ", to:" + to + " )", "amarelo");

		if(from.equals("/.statistics.txt"))
			return 0;

		numOp[6]++;

		try {
			long time = System.currentTimeMillis();
			NodeMetadata fromMeta = directoryService.getMetadata(from);
			Statistics.incGetMeta(System.currentTimeMillis()-time);
			String[] vec = dividePath(to);
			int nlink = fromMeta.getStats().getNlink()+1;
			fromMeta.getStats().setNlink(nlink);
			NodeMetadata m = new NodeMetadata(fromMeta.getNodeType(), vec[0], vec[1], fromMeta.getStats(), fromMeta.getId_path(), fromMeta.getKey(), null, null);
			time = System.currentTimeMillis();
			directoryService.putMetadata(m);
			Statistics.incPutMeta(System.currentTimeMillis()-time);
			if(nlink>2){
				time = System.currentTimeMillis();
				Collection<NodeMetadata> l = directoryService.getAllLinks(fromMeta.getId_path());
				Statistics.incGetAllLinksMeta(System.currentTimeMillis() - time);
				for(NodeMetadata meta : l){
					FileStats fs = meta.getStats();
					fs.setNlink(nlink);
					NodeMetadata nodeMeta = new NodeMetadata(meta.getNodeType(), meta.getParent(), meta.getName(), fs, meta.getId_path(), meta.getKey(), null, null);
					time = System.currentTimeMillis();
					directoryService.updateMetadata(meta.getPath(), nodeMeta);
					Statistics.incUpdateMeta(System.currentTimeMillis()-time);
				}
			}
		} catch (DirectoryServiceConnectionProblemException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		} catch (DirectoryServiceException e) {
			e.printStackTrace();
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		}

		return 0;
	}

	@Override
	public int release(String path, Object fh, int flags) throws FuseException {
		Printer.println("\n :::: RELEASE ( " + path + ", flags: " + flags + " )", "amarelo");

		numOp[12]++;
		if(path.equals("/.statistics.txt"))
			return 0;

		NodeMetadata nm = ((NodeMetadata)fh);
		daS.cleanMemory(nm.getId_path());


		if(!config.isNonSharing()){
			if (lockedFiles.containsKey(nm.getId_path()) && (flags & SCFSConstants.O_ACCMODE) == O_WRONLY || (flags & SCFSConstants.O_ACCMODE) == O_RDWR) {	
				daS.releaseData(nm.getId_path());
				lockedFiles.remove(nm.getId_path());
			}
		}

		return 0;
	}

	@Override
	public int truncate(String path, long size) throws FuseException {
		Printer.println("\n :::: TRUNCATE(" + path + ", size:" + size + " )", "amarelo");

		numOp[17]++;

		if(path.equals("/.statistics.txt"))
			return 0;

		NodeMetadata metadata = null;

		if(!config.isNonSharing()){
			metadata=this.getMetadata(path);

			try {
				FileStats fs = metadata.getStats();
				fs.setBlocks((int) ((size + 511L) / 512L));
				fs.setSize(size);

				daS.truncData(metadata.getId_path(), (int)size, metadata.getKey(), metadata.getStats().getDataHash(), false, metadata.getStats().isPending());
				NodeMetadata node = new NodeMetadata(metadata.getNodeType(), metadata.getParent(), metadata.getName(), fs, metadata.getId_path(), metadata.getKey(), metadata.getC_r(), metadata.getC_w());
				if(metadata.getStats().isPrivate()){
					namespace.updateMetadata(path, node);
				}else{
					long time = System.currentTimeMillis();
					directoryService.updateMetadata(path, node);
					Statistics.incUpdateMeta(System.currentTimeMillis() - time);
				}

			} catch (DirectoryServiceConnectionProblemException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			} catch (DirectoryServiceException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			}

		}else{
			try {
				long time = System.currentTimeMillis();
				metadata = directoryService.getMetadata(path);
				Statistics.incGetMeta(System.currentTimeMillis() - time);

				FileStats fs = metadata.getStats();
				fs.setBlocks((int) ((size + 511L) / 512L));
				fs.setSize(size);

				daS.truncData(metadata.getId_path(), (int)size, metadata.getKey(), metadata.getStats().getDataHash(), false, metadata.getStats().isPending());
				NodeMetadata node = new NodeMetadata(metadata.getNodeType(), metadata.getParent(), metadata.getName(), fs, metadata.getId_path(), metadata.getKey(), metadata.getC_r(), metadata.getC_w());
				time = System.currentTimeMillis();
				directoryService.updateMetadata(path, node);
				Statistics.incUpdateMeta(System.currentTimeMillis() - time);
			} catch (DirectoryServiceException e) {
			}
		}

		return 0;
	}

	@Override
	public int rmdir(String path) throws FuseException {
		Printer.println("\n :::: RMDIR(" + path + " )", "amarelo");
		numOp[14]++;
		if(!config.isNonSharing()){
			try {
				namespace.removeMetadata(path);
			} catch (DirectoryServiceConnectionProblemException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			} catch (DirectoryServiceException e) {
				try {
					long time = System.currentTimeMillis();
					directoryService.removeMetadata(path);
					Statistics.incDelMeta(System.currentTimeMillis()-time);
				} catch (DirectoryServiceException e1) {
					throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
				}
			}
		}else{
			try {
				long time = System.currentTimeMillis();
				directoryService.removeMetadata(path);
				Statistics.incDelMeta(System.currentTimeMillis()-time);
			} catch (DirectoryServiceException e1) {
				throw new FuseException(e1.getMessage()).initErrno(FuseException.ECONNABORTED);
			}
		}

		return 0;
	}

	@Override
	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
		numOp[15]++;
		Printer.println("\n :::: STATFS()", "amarelo");
		statfsSetter.set(statfs.blockSize, statfs.blocks, statfs.blocksFree, statfs.blocksAvail, statfs.files, statfs.filesFree, statfs.namelen);
		return 0;
	}

	@Override
	public int symlink(String from, String to) throws FuseException {
		numOp[16]++;

		Printer.println("\n :::: SYMLINK(from: " + from + ", to:" + to + " )", "ciao");
		if(from.equals("/.statistics.txt"))
			return 0;

		try {

			String[] vec = dividePath(to);
			FileStats fs = createDefaultFileStats(NodeType.SYMLINK, 0, -1);
			NodeMetadata m = new NodeMetadata(NodeType.SYMLINK, vec[0], vec[1], fs, from, defaultKey, null, null);

			directoryService.putMetadata(m);
		} catch (DirectoryServiceConnectionProblemException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		} catch (DirectoryServiceException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		}

		return 0;
	}

	@Override
	public int readlink(String path, CharBuffer link) throws FuseException {
		Printer.println("\n :::: READLINK(" + path + ", link: " + link + " )", "amarelo");

		numOp[11]++;
		NodeMetadata nm = null;
		try {
			nm = namespace.getMetadata(path);
		} catch (DirectoryServiceConnectionProblemException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		} catch (DirectoryServiceException e) {
			try {
				long time = System.currentTimeMillis();
				nm = directoryService.getMetadata(path);
				Statistics.incGetMeta(System.currentTimeMillis() - time);
			} catch (DirectoryServiceException e1) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			}
		}
		link.put(nm.getId_path());

		return 0;
	}

	@Override
	public int unlink(String path) throws FuseException {
		Printer.println("\n :::: UNLINK( " + path + " )", "amarelo");

		numOp[18]++;

		if(path.equals("/.statistics.txt"))
			return 0;
		NodeMetadata metadata = null;
		boolean isInPNS = false;
		if(!config.isNonSharing()){
			try {
				metadata = namespace.getMetadata(path);
				isInPNS = true;
			} catch (DirectoryServiceException e1) {
				try {
					long time = System.currentTimeMillis();
					metadata = directoryService.getMetadata(path);
					Statistics.incGetMeta(System.currentTimeMillis() - time);
					isInPNS = false;
				} catch (DirectoryServiceException e) {
					throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
				}
			}
		}else{
			try {
				long time = System.currentTimeMillis();
				metadata = directoryService.getMetadata(path);
				Statistics.incGetMeta(System.currentTimeMillis() - time);
			} catch (DirectoryServiceException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			}
		}

		if(metadata.getStats().getNlink() == 1)
			daS.deleteData(metadata.getId_path());

		try {
			if(!config.isNonSharing() && isInPNS){
				namespace.removeMetadata(path);
			}else{
				long time = System.currentTimeMillis();
				directoryService.removeMetadata(path);
				Statistics.incDelMeta(System.currentTimeMillis()-time);
			}
		} catch (DirectoryServiceConnectionProblemException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		} catch (DirectoryServiceException e) {
			e.printStackTrace();
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		}

		return 0;
	}

	@Override
	public int utime(String path, int atime, int mtime) throws FuseException {

		numOp[19]++;

		Printer.println("\n :::: UTIME(" + path + ", atime: " + atime + ", mtime: " + mtime + " )", "amarelo");
		if(path.equals("/.statistics.txt"))
			return 0;

		NodeMetadata metadata = null;

		if(!config.isNonSharing()){

			metadata = this.getMetadata(path);
		}else{
			try {
				long time = System.currentTimeMillis();
				metadata = directoryService.getMetadata(path);
				Statistics.incGetMeta(System.currentTimeMillis() - time);
			} catch (DirectoryServiceException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			}
		}

		FileStats fs = metadata.getStats();
		fs.setAtime(atime);
		fs.setMtime(mtime);
		NodeMetadata node = new NodeMetadata(metadata.getNodeType(), metadata.getParent(), metadata.getName(), fs, metadata.getId_path(), metadata.getKey(), metadata.getC_r(), metadata.getC_w());

		try {
			if(!config.isNonSharing() && metadata.getStats().isPrivate()){
				namespace.updateMetadata(path, node);
			}else{
				long time = System.currentTimeMillis();
				directoryService.updateMetadata(path, node);
				Statistics.incUpdateMeta(System.currentTimeMillis() - time);
			}

		} catch (DirectoryServiceConnectionProblemException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		} catch (DirectoryServiceException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		}

		return 0;
	}

	@Override
	public int chown(String path, int uid, int gid) throws FuseException {
		Printer.println("\n :::: CHOWN(" + path + "uid: " + uid + ", gid: " + gid + " )", "amarelo");
		if(path.equals("/.statistics.txt"))
			return 0;

		numOp[1]++;

		NodeMetadata metadata=null;

		if(!config.isNonSharing()){
			metadata = this.getMetadata(path);
		}else{
			try {
				long time = System.currentTimeMillis();
				metadata = directoryService.getMetadata(path);
				Statistics.incGetMeta(System.currentTimeMillis() - time);
			} catch (DirectoryServiceException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
			}
		}

		FileStats fs = metadata.getStats();
		fs.setUid(uid);
		fs.setGid(gid);
		NodeMetadata node = new NodeMetadata(metadata.getNodeType(), metadata.getParent(), metadata.getName(), fs, metadata.getId_path(), metadata.getKey(), metadata.getC_r(), metadata.getC_w());

		try {
			if(!config.isNonSharing() && metadata.getStats().isPrivate()){
				namespace.updateMetadata(path, node);
			}else{
				long time = System.currentTimeMillis();
				directoryService.updateMetadata(path, node);
				Statistics.incUpdateMeta(System.currentTimeMillis() - time);
			}

		} catch (DirectoryServiceConnectionProblemException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		} catch (DirectoryServiceException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.ECONNABORTED);
		}
		return 0;
	}


	private String getNextIdPath(){
		return String.valueOf(clientId) + System.currentTimeMillis() + (fileNum++ % 500);
	}

	public static FileStats createDefaultFileStats(NodeType nodeType, long inode, int mode) {
		if(nodeType == NodeType.SYMLINK)
			mode = FuseFtype.TYPE_SYMLINK | 0777;
		else if(nodeType == NodeType.DIR)
			mode = FuseFtype.TYPE_DIR | 0755;
		else
			mode = FuseFtype.TYPE_FILE | 0644;

		int nlink = 1;
		int uid = Integer.parseInt(System.getProperty("uid")); 
		int gid = Integer.parseInt(System.getProperty("gid"));
		int rdev = 0;
		int size = 0;
		long blocks = 0;
		int currentTime = (int) (System.currentTimeMillis() / 1000L);
		int atime = currentTime, mtime = currentTime, ctime = currentTime;
		return new FileStats(inode, mode, nlink, uid, gid, rdev, size, blocks, atime, mtime, ctime, emptyHash, nodeType == NodeType.FILE ? true : false, true);
	}

	@Override
	public int getxattr(String path, String name, ByteBuffer buf)
			throws FuseException, BufferOverflowException {
		Printer.println("\n :::: GETXATTR(" + path + ", name: " + name + " )", "amarelo");

		NodeMetadata m = null;
		if(!config.isNonSharing()){
			m = this.getMetadata(path);

			byte[] array = m.getStats().getXattr().get(name);
			if(array!=null){
				buf.put(array);
			}

		}
		return 0;
	}

	@Override
	public int getxattrsize(String path, String name, FuseSizeSetter setter)
			throws FuseException {
		Printer.println("\n :::: GETXATTRSIZE(" + path + ", name: " + name + " )", "amarelo");

		NodeMetadata m = null;
		if(!config.isNonSharing()){
			m = this.getMetadata(path);
			byte[] array = m.getStats().getXattr().get(name);
			if(array!=null)
				setter.setSize(array.length);
		}
		return 0;
	}

	@Override
	public int listxattr(String path, XattrLister list) throws FuseException {
		Printer.println("\n :::: LISTXATTR(" + path + " )", "amarelo");

		NodeMetadata m = null;
		if(!config.isNonSharing()){
			m = this.getMetadata(path);
			for(String name : m.getStats().getXattr().keySet())
				list.add(name);
		}
		return 0;
	}

	@Override
	public int removexattr(String path, String name) throws FuseException {
		Printer.println("\n :::: REMOVEXATTR(" + path + ", name: " + name + " )", "amarelo");

		NodeMetadata m = null;
		if(!config.isNonSharing()){
			m=this.getMetadata(path);
		}
		byte[] array = m.getStats().getXattr().remove(name);
		if(array!=null){

			try {
				if(m.getStats().isPrivate())
					namespace.updateMetadata(path, m);
				else
					directoryService.updateMetadata(path, m);
			} catch (DirectoryServiceException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.ENOENT);
			}
		}
		return 0;
	}

	@Override
	public int setxattr(String path, String name, ByteBuffer value, int flags)
			throws FuseException {
		value.order(ByteOrder.LITTLE_ENDIAN);
		boolean updated = false;
		byte[] buff = new byte[value.remaining()];
		value.get(buff);

		value.rewind();
		value.getInt(); value.getInt(); value.getInt(); value.getShort();
		int perm = value.getShort();
		int id = value.getInt();

		try {
			boolean inPNS = true;
			NodeMetadata m = null;
			if(!config.isNonSharing()){
				try {
					m = namespace.getMetadata(path);
				} catch (DirectoryServiceException e) {
					inPNS = false;
				}
			}else{
				inPNS=false;
			}
			if(!inPNS)
				m = directoryService.getMetadata(path);
			m.getStats().getXattr().put(name, buff);


			LinkedList<Pair<String, String>> cannonicalIds=new LinkedList<Pair<String,String>>();
			if(!config.isNonSharing()){
				try{
					List<String[]> l = directoryService.getCredentials(id);
					for(String[] sx : l)
						cannonicalIds.add(new Pair<String, String>(sx[0], sx[1]));
				}catch (DirectoryServiceConnectionProblemException e){
					System.err.println("DirectoryServiceConnectionProblemException - getCredential.");
				}catch (DirectoryServiceException e){
					System.err.println("DirectoryServiceException - getCredential.");
				}


				if(perm!=1){
					if(perm == 4 || perm == 5){
						daS.setPermition( m.getId_path(),"r", cannonicalIds);
						updated=privateToPublic(path, m, id);
					}else if(perm == 2 || perm == 3){
						daS.setPermition( m.getId_path(),"w", cannonicalIds);
						updated=privateToPublic(path, m, id);

					}else if(perm == 0){
						daS.setPermition( m.getId_path(),"", cannonicalIds);
						if(!m.getStats().isPrivate())
							updated=publicToPrivate(path, m, id);
					}else{
						daS.setPermition( m.getId_path(), "rw",cannonicalIds);
						updated=privateToPublic(path, m, id);
					}
				}else{
					daS.setPermition( m.getId_path(),"",cannonicalIds);
					updated=publicToPrivate(path, m, id);
				}
			}

			if(!updated){
				if(!inPNS)
					directoryService.updateMetadata(path, m);
				else
					namespace.updateMetadata(path, m);
			}
		} catch (DirectoryServiceException e) {
			e.printStackTrace();
			throw new FuseException(e.getMessage()).initErrno(FuseException.ENOENT);
		}
		return 0;
	}

	private String getOpsString(){
		StringBuilder opsStr = new StringBuilder("FUSE Ops:\n");
		for(int i=0;i<numOp.length;i++){
			opsStr.append("\t- " + ops[i].concat("\t\t")+ numOp[i]+"\n");
		}
		return opsStr.append("\n").toString();
	}

	private boolean privateToPublic(String path, NodeMetadata m, int id) throws DirectoryServiceException {
		FileStats fs = m.getStats();
		fs.setPrivate(false);
		m.setStats(fs);


		if(!arrayContains(m.getC_r(), id)){
			int[] cr = m.getC_r() == null ? new int[1] : Arrays.copyOf(m.getC_r(), m.getC_r().length+1);
			cr[cr.length-1] = id;
			m.setC_r(cr);
		}

		if(!arrayContains(m.getC_w(), id)){
			int[] cw = m.getC_w() == null ? new int[1] : Arrays.copyOf(m.getC_w(), m.getC_w().length+1);
			cw[cw.length-1] = id;
			m.setC_w(cw);
		}

		if(m.getC_r().length>1 || m.getC_w().length>1 ){
			directoryService.updateMetadata(path, m);
			return true;
		}


		return false;
	}

	/**
	 * 
	 * @param path
	 * @param m
	 * @param id
	 * @return true if the file was pushed to PNS.
	 * @throws DirectoryServiceException if file already exists in PNS.
	 */
	private boolean publicToPrivate(String path, NodeMetadata m, int id) throws DirectoryServiceException{
		if(id==clientId)
			return false;

		FileStats fs = m.getStats();
		fs.setPrivate(true);
		m.setStats(fs);
		if(m.getC_r().length>0 && arrayContains(m.getC_r(), id)){
			int[] cr = new int[m.getC_r().length-1];
			for(int i=0; i<cr.length ; i++)
				if(m.getC_r()[i]!=id)
					cr[i] = m.getC_r()[i];
			m.setC_r(cr);
		}

		if(m.getC_w().length>0){
			int[] cw = new int[m.getC_w().length-1];
			for(int i=0; i<cw.length ; i++)
				if(m.getC_w()[i]!=id)
					cw[i] = m.getC_w()[i];
			m.setC_w(cw);
		}

		if(m.getC_r().length==1 || m.getC_w().length==1){
			namespace.putMetadata(m);
			directoryService.removeMetadata(path);
			return true;
		}
		return false;
	}

	private List<String[][]> readCredentials() throws FileNotFoundException, ParseException{
		Scanner sc=new Scanner(new File("config"+File.separator+"accounts.properties"));
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


	private boolean arrayContains(int[] array, int value){
		if(array==null)
			return false;
		for(int i : array)
			if(i==value)
				return true;
		return false;

	}


	private String[] dividePath(String path) {
		if(path.equals("/"))
			return new String [] {"root", ""};

		String[] toRet = new String[2];
		String[] split = path.split("/");
		toRet[1] = split[split.length-1];
		if(split.length == 2)
			toRet[0] = path.substring(0, path.length()-toRet[1].length());
		else
			toRet[0] = path.substring(0, path.length()-toRet[1].length()-1);
		return toRet;
	}

	private DepSpaceAccessor init(String tsName, boolean confidencial, int clientId) {
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


	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new ShutDownThread(args[2], Thread.currentThread()));
		Log l = LogFactory.getLog(SCFS.class);


		String fuseArgs[] = new String[3];
		String C2FSArgs[] = new String[args.length-2];
		System.arraycopy(args, 0, fuseArgs, 0, fuseArgs.length);
		System.arraycopy(args, 2, C2FSArgs, 0, C2FSArgs.length);

		Configure config = new Configure(C2FSArgs[0], new Integer(C2FSArgs[1]));
		boolean goodMount = config.setConfiguration(C2FSArgs);
		Printer.println("setConfig Result -> " + goodMount);
		if(goodMount){
			File f = new File(C2FSArgs[0]);
			if (!f.exists())
				while (!f.mkdir())
					;

			try {
				FuseMount.mount(fuseArgs, new SCFS(config), l);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

class ShutDownThread extends Thread {
	private String mountPoint;
	private Thread thread;
	public ShutDownThread(String mountPoint, Thread thread) {
		this.thread = thread;
		this.mountPoint = mountPoint;
	}

	@SuppressWarnings("deprecation")
	public void run() {
		System.out.println("\n\nSCFS ShutDown!!\nPlease authenticate your sudo account to umount the system.\nMake sure you are not using the mountPoint right now.");
		thread.stop();
		try {
			Thread.sleep(100);
			Runtime.getRuntime().exec("gksudo -g -D SCFS_umount umount " + mountPoint);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

