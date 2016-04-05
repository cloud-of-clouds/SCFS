package scfs.directoryService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import depskys.core.MyAESCipher;
import fuse.FuseFtype;
import scfs.directoryService.exceptions.DirectoryServiceConnectionProblemException;
import scfs.directoryService.exceptions.DirectoryServiceException;
import scfs.general.NodeType;
import scfs.general.Printer;
import scfs.general.SCFS;
import scfs.general.Statistics;

public class ZookeeperDirectoryService implements DirectoryService {

	private static final String ROOT_PARENT = "scfs";
	private ZooKeeper zk;
	private int clientId;
	private SecretKey defaultKey;
	private Map<String, NodeMetadata> buffer;
	private int cont;
	private String parentZnode;
	private String inodesZnode;


	public ZookeeperDirectoryService(int clientId, ZooKeeper zk)  {
		this.zk = zk;
		this.cont=0;
		this.clientId = clientId;
		this.buffer = new HashMap<String, NodeMetadata>();
		this.parentZnode = "/" + ROOT_PARENT + "-" + clientId;
		this.inodesZnode = parentZnode + "-inodes/";
		try {
			this.defaultKey = MyAESCipher.generateSecretKey();
		} catch (Exception e) {	e.printStackTrace(); }

		byte[] contentRoot = getAsZnodeContent(new NodeMetadata(NodeType.DIR, "/", ROOT_PARENT + "-" + clientId, createDefaultFileStats(true, Long.parseLong(getNextIdPath())), getNextIdPath(), defaultKey, null, null));
		byte[] contentInode = getAsZnodeContent(new NodeMetadata(NodeType.DIR, "/", ROOT_PARENT + "-" + clientId + "-inodes", createDefaultFileStats(true, Long.parseLong(getNextIdPath())), getNextIdPath(), defaultKey, null, null));
		for(int cont = 0 ; cont<3; cont++){
			try {

				zk.transaction()
				.create(parentZnode, contentRoot, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
				.create(inodesZnode.substring(0, inodesZnode.lastIndexOf("/")), contentInode, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
				.commit();

				return;
			} catch (KeeperException e) {
				if(e.code()==Code.NODEEXISTS)
					return;
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


		System.out.println("(-) Some error occured. Cannot continue...");
		System.exit(2);
	}

	@Override
	public void putMetadata(NodeMetadata metadata) throws DirectoryServiceException {
		String path = metadata.getPath();
		String parsedPath = path.endsWith("/") ? path.substring(0, path.length()-1) : path;
		try{
			try {
				long time = System.currentTimeMillis();
				Printer.println("  -> Start operation create at Zookeeper", "azul");

				zk.transaction()
				.create(parentZnode+parsedPath, getAsZnodeContent(metadata), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
				.create(inodesZnode+metadata.getId_path(), metadata.getPath().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
				.commit();

				//			zk.create(parentZnode+metadata.getPath(), getAsZnodeContent(metadata), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				long tempo = System.currentTimeMillis()-time;
				Printer.println("  -> End operation create at Zookeeper", "azul");
				Printer.println("  -> Operation create took: " + Long.toString(tempo) + " milis", "azul");

				Statistics.incCas(System.currentTimeMillis()-time);
			} catch (KeeperException e) {
				try {
					if(zk.exists(parentZnode+parsedPath, false)!=null)
						throw new DirectoryServiceException("Node already exists");

					Stat stat = zk.exists(inodesZnode+metadata.getId_path(), false);

					if(stat==null)
						throw new DirectoryServiceConnectionProblemException("Some Problem occurred.");

					for(int i = 0 ; i<3; i++){
						// UPDATE NEW VERSION!
						try {
							String data = new String(zk.getData(inodesZnode+metadata.getId_path(), false, stat), "UTF-8");
							zk.transaction()
							.create(parentZnode+parsedPath, getAsZnodeContent(metadata), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
							.setData(inodesZnode+metadata.getId_path(), (data + ":" + metadata.getPath()).getBytes(), stat.getVersion())
							.commit();
							return;
						} catch (KeeperException e1) {
							System.out.println("(-) putMetadata: " + e.getMessage() + " -> retrying...");
							continue;
						}
					}
				} catch (KeeperException e1) {
					throw new DirectoryServiceConnectionProblemException("Some Problem occurred.");
				}
				throw new DirectoryServiceConnectionProblemException("Some Problem occur.");
			}
		} catch (UnsupportedEncodingException | InterruptedException e1) {
			e1.printStackTrace();
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
	}

	@Override
	public void updateMetadata(String path, NodeMetadata metadata) throws DirectoryServiceException {

		String parsedPath = path.endsWith("/") ? path.substring(0, path.length()-1) : path;

		// handle RENAME.
		if(!path.equals(metadata.getPath())){
			rename(path, metadata);
			return;
		}

		for(int cont=0; cont<3 ;cont++){
			try {
				long time = System.currentTimeMillis();
				Printer.println("  -> Start operation setData at Zookeeper", "azul");
				zk.setData(parentZnode+parsedPath, getAsZnodeContent(metadata), -1);
				long tempo = System.currentTimeMillis()-time;
				Printer.println("  -> End operation setData at Zookeeper", "azul");
				Printer.println("  -> Operation setData took: " + Long.toString(tempo) + " milis", "azul");
				Statistics.incReplace(System.currentTimeMillis()-time);
				return;
			} catch (KeeperException e) {
				if(e.code() == Code.NONODE)
					throw new DirectoryServiceException("Node does not exist");
				if(e.code() == Code.BADVERSION){
					System.out.println("(-) updateMetadata: badVersion");
					continue;
				}
				throw new DirectoryServiceConnectionProblemException("Some Problem occured.");
			} catch (InterruptedException e) {
				throw new DirectoryServiceConnectionProblemException("Connection Problem.");
			}
		}
		throw new DirectoryServiceException("Too many invalid versions.");
	}

	private void rename(String path, NodeMetadata metadata) throws DirectoryServiceException {
		List<Op> ops = new ArrayList<Op>();

		getAllChildren(path, metadata, ops);

		try {
			zk.multi(ops);
		} catch (KeeperException e ) {
			if(e.code() == Code.NODEEXISTS){
				// overwrite file
				String parsedPath = metadata.getPath().endsWith("/") ? metadata.getPath().substring(0, metadata.getPath().length()-1) : metadata.getPath();
				String parsedPathFrom = path.endsWith("/") ? path.substring(0, path.length()-1) : path;

				
				try {
					Stat stat = zk.exists(inodesZnode+metadata.getId_path(), false);
					String inode = new String(zk.getData(inodesZnode+metadata.getId_path(), false , stat), "UTF-8");
					//update info
					inode = inode.replace(parsedPathFrom, parsedPath);
					zk.transaction()
					.delete(parentZnode+parsedPath, -1)
					.create(parentZnode+parsedPath, getAsZnodeContent(metadata), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
					.setData(inodesZnode+metadata.getId_path(), inode.getBytes(), -1)
					.delete(parentZnode+parsedPathFrom, -1)
					.commit();
				} catch (InterruptedException | KeeperException e1) {
					System.out.println("(--)" + e.getMessage());
					throw new DirectoryServiceConnectionProblemException(e.getMessage());
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
			}else{
				System.out.println("(-)" + e.getMessage());
				throw new DirectoryServiceConnectionProblemException(e.getMessage());
			}
		} catch (InterruptedException e) {
			System.out.println("(-)" + e.getMessage());
			throw new DirectoryServiceConnectionProblemException(e.getMessage());
		}

	}

	private void getAllChildren(String path, NodeMetadata metadata, List<Op> ops) throws DirectoryServiceException{
		String parsedPath = metadata.getPath().endsWith("/") ? metadata.getPath().substring(0, metadata.getPath().length()-1) : metadata.getPath();
		
		
		ops.add(Op.create(parentZnode+parsedPath, getAsZnodeContent(metadata), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));

		if(metadata.isDirectory()){
			Collection<NodeMetadata> res = getNodeChildren(path);

			for(NodeMetadata m : res){
				String oldPath = m.getPath();
				m.setParent(metadata.getPath());
				getAllChildren(oldPath, m, ops);
			}
		}else{
			try {
				Stat stat = zk.exists(inodesZnode+metadata.getId_path(), false);
				String inode = new String(zk.getData(inodesZnode+metadata.getId_path(), false , stat), "UTF-8");
				//update info
				inode = inode.replace(path, parsedPath);
				ops.add(Op.setData(inodesZnode+metadata.getId_path(), inode.getBytes(), -1));
			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		parsedPath = path.endsWith("/") ? path.substring(0, path.length()-1) : path;

		ops.add(Op.delete(parentZnode+parsedPath, -1));

	}

	@Override
	public void commitMetadataBuffer(String path, byte[] hash) throws DirectoryServiceException {
		System.out.println("commitmetadata - " + path + ", " + SCFS.getHexHash(hash));
		synchronized (buffer) {
			if(buffer.containsKey(path))
				updateMetadata(path, buffer.get(path));
		}
	}

	@Override
	public void insertMetadataInBuffer(String path, NodeMetadata metadata) throws DirectoryServiceException {
		synchronized (buffer) {
			buffer.put(path, metadata);
		}
	}

	@Override
	public void removeMetadata(String path) throws DirectoryServiceException {
		String parsedPath = path.endsWith("/") ? path.substring(0, path.length()-1) : path;
		for(int i = 0 ; i < 3 ; i++){
			try {
				long time = System.currentTimeMillis();
				Printer.println("  -> Start operation delete at Zookeeper", "azul");
				String idPath = getMetadataFromZnodeContent(zk.getData(parentZnode+parsedPath, false, null)).getId_path();
				zk.transaction()
				.delete(parentZnode+parsedPath, -1)
				.delete(inodesZnode + idPath, -1)
				.commit();
				long tempo = System.currentTimeMillis()-time;
				Printer.println("  -> End operation delete at Zookeeper", "azul");
				Printer.println("  -> Operation delete took: " + Long.toString(tempo) + " milis", "azul");
				Statistics.incInp(System.currentTimeMillis()-time);
				return;
			} catch (InterruptedException e) {
				throw new DirectoryServiceConnectionProblemException("Conection Problem");
			} catch (KeeperException e) {
				if(e.code() == Code.BADVERSION){
					System.out.println("(-) removeMetadata: badVersion");
					continue;
				}
				if(e.code() != Code.NONODE)
					e.printStackTrace();

				break;
			}
		}
		throw new DirectoryServiceConnectionProblemException("Too many attempts with bad version.");
	}

	@Override
	public NodeMetadata getMetadata(String path) throws DirectoryServiceException {
		if((cont++) == 50){
			cont=0;
			System.out.print(".");
		}

		String parsedPath = path.endsWith("/") ? path.substring(0, path.length()-1) : path;
		byte[] res = null;
		for(int i = 0; i<3;i++){
			try {
				long time = System.currentTimeMillis();
				Printer.println("  -> Start operation getData at Zookeeper", "azul");
				res = zk.getData(parentZnode+parsedPath, false, null);
				long tempo = System.currentTimeMillis()-time;
				Printer.println("  -> End operation getData at Zookeeper", "azul");
				Printer.println("  -> Operation getData took: " + Long.toString(tempo) + " milis", "azul");
				Statistics.incRdp(System.currentTimeMillis()-time);
				return getMetadataFromZnodeContent(res);
			} catch (KeeperException e) {
				if(e.code()==Code.NONODE)
					throw new DirectoryServiceException("znode not Found!");
				if(e.code()==Code.BADVERSION){
					System.out.println("(-) getMetadata: bad version.");
					continue;
				}
				e.printStackTrace();
				throw new DirectoryServiceConnectionProblemException("Some problem occurs.");
			} catch (InterruptedException e) {
				throw new DirectoryServiceConnectionProblemException("Connection Problem");
			}
		}
		throw new DirectoryServiceConnectionProblemException("Too many attemps with bad version.");
	}

	@Override
	public Collection<NodeMetadata> getNodeChildren(String path) throws DirectoryServiceException {
		String parsedPath = path.endsWith("/") ? path.substring(0, path.length()-1):path;
		for(int i = 0 ; i<3 ; i++){
			try {
				long time = System.currentTimeMillis();
				Printer.println("  -> Start operation getChildren at Zookeeper", "azul");
				List<String> list = zk.getChildren(parentZnode+parsedPath, false, null);
				List<NodeMetadata> ops = new ArrayList<NodeMetadata>();
				Statistics.incRdall(System.currentTimeMillis()-time);
				for(String node : list){
					try{
						ops.add(getMetadataFromZnodeContent(zk.getData(parentZnode+parsedPath+"/"+node, false, null)));
					} catch (Exception e) {
						System.out.println("(-) getNodeChildren: getting " + node + " - " + e.getMessage());
					}
				}
				long tempo = System.currentTimeMillis()-time;
				Printer.println("  -> End operation rgetChildren at Zookeeper", "azul");
				Printer.println("  -> Operation getChildren took: " + Long.toString(tempo) + " milis", "azul");
				return ops;
			} catch (KeeperException e) {
				if(e.code()==Code.NONODE)
					throw new DirectoryServiceException("znode not Found!");
				if(e.code()==Code.BADVERSION){
					System.out.println("(-) getMetadata: bad version.");
					continue;
				}	
				e.printStackTrace();
			} catch (InterruptedException e) {
				throw new DirectoryServiceConnectionProblemException("Connection Problem");
			}

		}
		throw new DirectoryServiceConnectionProblemException("Too many attemps with bad version.");

	}

	@Override
	public Collection<NodeMetadata> getAllLinks(String idPath) throws DirectoryServiceException {
		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation getData at Zookeeper", "azul");
			String[] data = new String(zk.getData(inodesZnode+idPath, false, null), "UTF-8").split(":");

			if(data == null || data.length < 1) {
				long tempo = System.currentTimeMillis()-time;
				Printer.println("  -> End operation getData at Zookeeper", "azul");
				Printer.println("  -> Operation getData took: " + Long.toString(tempo) + " milis", "azul");
				Statistics.incRdall(System.currentTimeMillis()-time);
				throw new DirectoryServiceException("Data not found");
			}else{
				Collection<NodeMetadata> res = new ArrayList<NodeMetadata>();
				for (String fName : data){
					String parsedPath = fName.endsWith("/") ? fName.substring(0, fName.length()-1):fName;
					try {
					res.add(getMetadataFromZnodeContent(zk.getData(parentZnode+parsedPath, false, null)));
					} catch (KeeperException e) {
						if(e.code() != Code.NONODE)
							throw e;
					}
				}

				long tempo = System.currentTimeMillis()-time;
				Printer.println("  -> End operation getData at Zookeeper", "azul");
				Printer.println("  -> Operation getData took: " + Long.toString(tempo) + " milis", "azul");
				Statistics.incRdall(System.currentTimeMillis()-time);

				return res;
			}
		} catch (KeeperException e) {
			if(e.code() == Code.NONODE)
				throw new DirectoryServiceException("No idPath mapper.");
			e.printStackTrace();
			throw new DirectoryServiceConnectionProblemException("Some problem occurred.");
		} catch (InterruptedException | UnsupportedEncodingException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
	}

	@Override
	public PrivateNameSpaceStats getPrivateNameSpaceMetadata() throws DirectoryServiceException {

		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation getData at Zookeeper", "azul");
			byte[] data = zk.getData("/scfs-pns/pns-"+clientId, false, null);
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation getData at Zookeeper", "azul");
			Printer.println("  -> Operation getData took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incRdp(System.currentTimeMillis()-time);

			if(data==null)
				throw new DirectoryServiceException("Node not exists.");

			return PrivateNameSpaceStats.getFromBytes(data);
		} catch (KeeperException e) {
			if(e.code() == Code.NONODE)
				throw new DirectoryServiceException("Node not exists.");
			e.printStackTrace();
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}

	}

	@Override
	public void putPrivateNameSpaceMetadata(int clientId, PrivateNameSpaceStats pnsStats)
			throws DirectoryServiceException {

		byte[] data = pnsStats.getBytes();
		if(data==null)
			throw new DirectoryServiceConnectionProblemException("(-) Malformed PrivateNameSpaceStats.");

		try {
			zk.create("/scfs-pns", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (KeeperException e) {
			if(e.code() != Code.NODEEXISTS){
				e.printStackTrace();
				throw new DirectoryServiceConnectionProblemException(e.getMessage());
			}
		} catch (InterruptedException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}

		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation create at Zookeeper", "azul");
			zk.create("/scfs-pns/pns-"+clientId, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation create at Zookeeper", "azul");
			Printer.println("  -> Operation create took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incCas(System.currentTimeMillis()-time);
		} catch (KeeperException e) {
			if(e.code() == Code.NODEEXISTS){
				e.printStackTrace();
				throw new DirectoryServiceException("Node already exists");
			}
			throw new DirectoryServiceConnectionProblemException(e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new DirectoryServiceConnectionProblemException(e.getMessage());
		}

	}

	@Override
	public void putCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException {
		try {
			zk.create("/scfs-credentials", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (KeeperException e) {
			if(e.code() != Code.NODEEXISTS){
				e.printStackTrace();
				throw new DirectoryServiceConnectionProblemException(e.getMessage());
			}
		} catch (InterruptedException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}

		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation create at Zookeeper", "azul");
			zk.create("/scfs-credentials/"+clientId, listToString(list).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation create at Zookeeper", "azul");
			Printer.println("  -> Operation create took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incCas(System.currentTimeMillis()-time);
		} catch (KeeperException e) {
			if(e.code() != Code.NODEEXISTS)
				throw new DirectoryServiceConnectionProblemException(e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new DirectoryServiceConnectionProblemException(e.getMessage());
		}

	}

	@Override
	public List<String[]> getCredentials(int clientId) throws DirectoryServiceException {

		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation getData at Zookeeper", "azul");
			byte[] data = zk.getData("/scfs-credentials/"+clientId, false, null);
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation getData at Zookeeper", "azul");
			Printer.println("  -> Operation getData took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incRdp(System.currentTimeMillis()-time);

			if(data==null)
				throw new DirectoryServiceException("Node not exists.");

			return stringToList(new String(data, "UTF-8"));
		} catch (KeeperException e) {
			e.printStackTrace();
			if(e.code() == Code.NONODE)
				throw new DirectoryServiceException("Node not exists.");
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		} catch (InterruptedException | UnsupportedEncodingException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
	}

	@Override
	public void updateCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException {
		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation setData at Zookeeper", "azul");
			zk.setData("/scfs-credentials/"+clientId, listToString(list).getBytes(), -1);
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation setData at Zookeeper", "azul");
			Printer.println("  -> Operation setData took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incCas(System.currentTimeMillis()-time);
		} catch (KeeperException e) {
			if(e.code() != Code.NODEEXISTS)
				throw new DirectoryServiceConnectionProblemException(e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new DirectoryServiceConnectionProblemException(e.getMessage());
		}
	}


	private String listToString(List<String[]> list){
		String listStr = new String();
		for(int i=0; i<list.size(); i++){
			listStr = listStr.concat(list.get(i)[0] + "\t" + list.get(i)[1]);
			if(i<list.size()-1)
				listStr=listStr.concat("\n");
		}
		return listStr;
	}

	private List<String[]> stringToList(String res){
		String[] vec = res.split("\n");
		List<String[]> list = new LinkedList<String[]>();
		for(String ss:vec)
			list.add(ss.split("\t"));

		return list;
	}


	private byte[] getAsZnodeContent(NodeMetadata m){
		try {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);

			m.writeExternal(oos);

			oos.close();
			baos.close();

			return baos.toByteArray();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private NodeMetadata getMetadataFromZnodeContent(byte[] data) {

		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ObjectInputStream ois;
			ois = new ObjectInputStream(bais);

			NodeMetadata res = new NodeMetadata();
			res.readExternal(ois);

			ois.close();
			bais.close();

			return res;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return null;
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

	private String getNextIdPath(){
		return String.valueOf(clientId) + System.currentTimeMillis();
	}

}
