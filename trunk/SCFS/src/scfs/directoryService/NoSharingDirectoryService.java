package scfs.directoryService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import scfs.directoryService.exceptions.DirectoryServiceConnectionProblemException;
import scfs.directoryService.exceptions.DirectoryServiceException;
import scfs.general.NodeType;
import scfs.storageService.StorageService;
import scfs.util.ExternalizableHashMap;
import depskys.core.MyAESCipher;
import fuse.FuseFtype;

public class NoSharingDirectoryService implements DirectoryService {

	private static final long NS_UPDATER_DELTA = 5000;
	private ConcurrentHashMap<String, NodeMetadata> metadataBag;
	private StorageService storageService;
	private String pathId;
	private SecretKey key;
	private ExternalizableHashMap lastUpdate;
	private Timer timer;

	private NSUpdaterTimerTask nsUpdater;


	public NoSharingDirectoryService(String idPath, SecretKey key, boolean isPNS) {
		this.key = key;
		this.pathId = idPath;
		this.lastUpdate = new ExternalizableHashMap();
		this.metadataBag = new ConcurrentHashMap<String, NodeMetadata>();
		this.timer = new Timer();
	}

	public void setStorageService(StorageService daS) throws IOException{
		this.storageService = daS;
		this.nsUpdater = new NSUpdaterTimerTask(storageService.getAccessor(), key, pathId);
		this.initBag();
	}

	public NodeMetadata getMetadata(String path) throws DirectoryServiceException{
		NodeMetadata m = metadataBag.get(path);
		if(m != null)
			return m;
		throw new DirectoryServiceException("Node not exists");
	}

	public void putMetadata(NodeMetadata m) throws DirectoryServiceException{
		if(metadataBag.putIfAbsent(m.getPath(), m)!=null)
			throw new DirectoryServiceException("Node Already Exists!");
		writeMetadata();
	}

	public void removeMetadata(String path) throws DirectoryServiceException{
		if(metadataBag.remove(path)==null)
			throw new DirectoryServiceException("Node not exists.");
		writeMetadata();
	}

	public Collection<NodeMetadata> getNodeChildren(String path) throws DirectoryServiceException{
		if(!metadataBag.containsKey(path))
			throw new DirectoryServiceException("Node not exists");

		Collection<NodeMetadata> res = new LinkedList<NodeMetadata>();
		for(NodeMetadata m : metadataBag.values())
			if(m.getParent().equals(path))
				res.add(m);
		return res;
	}

	public void updateMetadata(String path, NodeMetadata m) throws DirectoryServiceException{
		if(metadataBag.remove(path)==null)
			throw new DirectoryServiceException("Node not exists.");
		metadataBag.put(m.getPath(), m);

		if(!path.equals(m.getPath())){
			for(NodeMetadata mdata : metadataBag.values()){
				if(mdata.getParent().equals(path)){
					String p = mdata.getPath();
					mdata.setParent(m.getPath());
					updateMetadata(p, mdata);
				}
			}
		}
		writeMetadata();
	}

	public Collection<NodeMetadata> getAllLinks(String idPath){
		Collection<NodeMetadata> res = new LinkedList<NodeMetadata>();
		Collection<NodeMetadata> list = metadataBag.values();
		for(NodeMetadata m : list)
			if(m.getId_path().equals(idPath))
				res.add(m);
		return res;
	}

	@Override
	public void insertMetadataInBuffer(String idPath, NodeMetadata metadata) throws DirectoryServiceException {
		NodeMetadata meta = this.metadataBag.get(metadata.getPath());
		if(meta != null){
			FileStats fs = meta.getStats();
			fs.setSize(metadata.getStats().getSize());
			fs.setDataHash(metadata.getStats().getDataHash());
			fs.setPending(metadata.getStats().isPending());
			meta.setStats(fs);

			NodeMetadata m =  new NodeMetadata(meta.getNodeType(), metadata.getParent(), metadata.getName(), fs, metadata.getId_path(), metadata.getKey(), metadata.getC_r(), metadata.getC_w());
			metadataBag.put(metadata.getPath(), m);
		}
	}

	@Override
	public void commitMetadataBuffer(String idPath, byte[] hash) throws DirectoryServiceException {
		if(!idPath.equals(this.pathId)){
			NodeMetadata meta = getMetadataByIdPath(idPath);
			if(meta == null) 
				return;
			FileStats fs = meta.getStats();
			fs.setDataHash(hash);
			meta.setStats(fs);

			NodeMetadata m =  new NodeMetadata(meta.getNodeType(), meta.getParent(), meta.getName(), fs, meta.getId_path(), meta.getKey(), meta.getC_r(), meta.getC_w());
			metadataBag.put(meta.getPath(), m);

			writeMetadata();
		}

	}

	@Override
	public PrivateNameSpaceStats getPrivateNameSpaceMetadata() throws DirectoryServiceException {
		//NOT IMPLEMENTED.
		return null;
	}

	@Override
	public void putPrivateNameSpaceMetadata(int clientId, PrivateNameSpaceStats pnsStats)
			throws DirectoryServiceException {
		//NOT IMPLEMENTED.
	}

	private void writeMetadata(){
		cloneBag();


		if(nsUpdater == null || !nsUpdater.isScheduled()){
			nsUpdater = new NSUpdaterTimerTask(storageService.getAccessor(), key, pathId);
			nsUpdater.schedule();
			nsUpdater.setLastUpdate(lastUpdate);
			timer.schedule(nsUpdater, NS_UPDATER_DELTA);
		}else
			nsUpdater.setLastUpdate(lastUpdate);

	}


	private void cloneBag() {
		lastUpdate.clear();
		lastUpdate.putAll(metadataBag);
	}

	private void initBag() throws IOException{

		byte [] buf = storageService.getAccessor().readFrom(pathId, key);

		if(buf!=null){
			boolean error = false;
			System.out.println("A NS was found.");

			metadataBag = new ConcurrentHashMap<String, NodeMetadata>();

			ByteArrayInputStream bais = new ByteArrayInputStream( buf );
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(bais);
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			}

			try {
				lastUpdate.readExternal(ois);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				throw new IOException();
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			}
			if(!error){
				metadataBag.putAll(lastUpdate);
			}
		}else{
			System.out.println("There is no NS.");
			this.metadataBag = new ConcurrentHashMap<String, NodeMetadata>();
			try {
				metadataBag.put("/", new NodeMetadata(new Object[] {NodeType.DIR.getAsString(),"root", "", createDefaultFileStats(true, 100), "", MyAESCipher.generateSecretKey()}, null, null));
			} catch (Exception e) {}
			writeMetadata();
		}

	}

	public NodeMetadata getMetadataByIdPath(String idPath){
		for(NodeMetadata m : metadataBag.values())
			if(m.getId_path().equals(idPath))
				return m;
		return null;
	}

	public boolean containsMetadata(String path){
		return metadataBag.contains(path);
	}

	private FileStats createDefaultFileStats(boolean isDir, long inode) {
		int mode = isDir ? FuseFtype.TYPE_DIR | 0755 : FuseFtype.TYPE_FILE | 0644;
		int nlink = 1;
		int uid = 0;
		int gid = 0;
		int rdev = 0;
		int size = 0;
		long blocks = 0;
		int atime = (int) 1000L, mtime = (int) 1000L, ctime = (int) 1000L;
		try {
			return new FileStats(inode, mode, nlink, uid, gid, rdev, size, blocks, atime, mtime, ctime, MessageDigest.getInstance("SHA-1").digest("".getBytes()), isDir ? false : true, true);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void putCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException {
		//DO NOTHING
	}

	@Override
	public List<String[]> getCredentials(int clientId) throws DirectoryServiceException {
		//DO NOTHING
		return new LinkedList<String[]>();
	}

	@Override
	public void updateCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException {
		//DO NOTHING
	}

}