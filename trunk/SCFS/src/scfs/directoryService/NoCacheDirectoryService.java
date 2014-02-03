package scfs.directoryService;
import fuse.FuseFtype;
import general.DepSpaceException;
import general.DepTuple;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import scfs.directoryService.exceptions.DirectoryServiceConnectionProblemException;
import scfs.directoryService.exceptions.DirectoryServiceException;
import scfs.general.NodeType;
import scfs.general.Printer;
import scfs.general.Statistics;
import client.DepSpaceAccessor;
import depskys.core.MyAESCipher;

public class NoCacheDirectoryService implements DirectoryService{

	private static final String ROOT_PARENT = "root";

	private DepSpaceAccessor accessor;
	private int clientId;
	private SecretKey defaultKey;
	private Map<String, NodeMetadata> buffer;
	private int cont;

	public NoCacheDirectoryService(int clientId, DepSpaceAccessor accessor) throws DepSpaceException {
		this.accessor = accessor;
		this.cont=0;
		this.clientId = clientId;
		this.buffer = new HashMap<String, NodeMetadata>();
		try {
			this.defaultKey = MyAESCipher.generateSecretKey();
		} catch (Exception e) {	e.printStackTrace(); }
		long time = System.currentTimeMillis();
		accessor.cas(DepTuple.createTuple(NodeType.DIR.getAsString() ,ROOT_PARENT, "", "*", "*", "*"), DepTuple.createTuple(NodeType.DIR.getAsString(),ROOT_PARENT, "", createDefaultFileStats(true, Long.parseLong(getNextIdPath())), "", defaultKey));
		Statistics.incCas(System.currentTimeMillis()-time);
	}

	@Override
	public void putMetadata(NodeMetadata metadata) throws DirectoryServiceException {
		DepTuple tuple;
		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation cas at DepSpace", "azul");
			tuple = accessor.cas(DepTuple.createTuple("*", metadata.getParent(), metadata.getName(), "*", "*", "*"), metadata.getAsTuple());
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation cas at DepSpace", "azul");
			Printer.println("  -> Operation cas took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incCas(System.currentTimeMillis()-time);
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
		if(tuple != null)
			throw new DirectoryServiceException("Node already exists");
	}

	@Override
	public void updateMetadata(String path, NodeMetadata metadata) throws DirectoryServiceException {
		String[] divPath = dividePath(path);
		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation replace at DepSpace", "azul");
			accessor.replace(DepTuple.createTuple("*", divPath[0], divPath[1], "*", "*","*"), metadata.getAsTuple());
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation replace at DepSpace", "azul");
			Printer.println("  -> Operation replace took: " + Long.toString(tempo) + " milis", "azul");

			Statistics.incReplace(System.currentTimeMillis()-time);
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
	}

	@Override
	public void removeMetadata(String path) throws DirectoryServiceException {
		String [] vec = dividePath(path);
		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation inp at DepSpace", "azul");
			accessor.inp(DepTuple.createTuple("*", vec[0], vec[1], "*", "*", "*"));
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation inp at DepSpace", "azul");
			Printer.println("  -> Operation inp took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incInp(System.currentTimeMillis()-time);
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Conection Problem");
		}
	}

	@Override
	public NodeMetadata getMetadata(String path) throws DirectoryServiceException {
		DepTuple tuple = null;
		try {
			if(cont++==50){
				cont=0;
				System.out.print(".");
			}
			String[] divPath = dividePath(path);
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation rdp at DepSpace", "azul");
			tuple = accessor.rdp(DepTuple.createTuple("*",divPath[0], divPath[1], "*", "*", "*"));
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation rdp at DepSpace", "azul");
			Printer.println("  -> Operation rdp took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incRdp(System.currentTimeMillis()-time);
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}

		if(tuple == null)
			throw new DirectoryServiceException("Tuple not Found!");

		NodeMetadata node = new NodeMetadata(tuple.getFields(), tuple.getC_rd(), tuple.getC_in());

		return node;
	}

	@Override
	public Collection<NodeMetadata> getNodeChildren(String path) throws DirectoryServiceException {

		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation rdAll at DepSpace", "azul");
			Collection<DepTuple> listTuples = accessor.rdAll(DepTuple.createTuple("*",path, "*", "*", "*", "*"), 0);
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation rdAll at DepSpace", "azul");
			Printer.println("  -> Operation rdAll took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incRdall(System.currentTimeMillis()-time);
			if(listTuples == null) 
				throw new DirectoryServiceException("Directory not found");
			else{
				Collection<NodeMetadata> res = new ArrayList<NodeMetadata>();
				for (DepTuple tup :  listTuples)
					res.add(new NodeMetadata(tup.getFields(), tup.getC_rd(), tup.getC_in()));

				return res;
			}
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
	}

	@Override
	public Collection<NodeMetadata> getAllLinks(String idPath) throws DirectoryServiceException {

		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation rdAll at DepSpace", "azul");
			Collection<DepTuple> listTuples = accessor.rdAll(DepTuple.createTuple("*","*", "*", "*", idPath, "*"), 0);
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation rdp at DepSpace", "azul");
			Printer.println("  -> Operation rdp took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incRdall(System.currentTimeMillis()-time);
			if(listTuples == null) 
				throw new DirectoryServiceException("Data not found");
			else{
				Collection<NodeMetadata> res = new ArrayList<NodeMetadata>();
				for (DepTuple tup : listTuples)
					res.add(new NodeMetadata(tup.getFields(), tup.getC_rd(), tup.getC_in()));

				return res;
			}
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
	}

	@Override
	public void commitMetadataBuffer(String path, byte[] hash) throws DirectoryServiceException {
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

	private String[] dividePath(String path) {
		if(path.equals("/"))
			return new String [] {ROOT_PARENT, ""};

		String[] toRet = new String[2];
		String[] split = path.split("/");
		toRet[1] = split[split.length-1];
		if(split.length == 2)
			toRet[0] = path.substring(0, path.length()-toRet[1].length());
		else
			toRet[0] = path.substring(0, path.length()-toRet[1].length()-1);
		return toRet;
	}

	private String getNextIdPath(){
		return String.valueOf(clientId) + System.currentTimeMillis();
	}

	@Override
	public PrivateNameSpaceStats getPrivateNameSpaceMetadata() throws DirectoryServiceException {
		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation rdp at DepSpace", "azul");
			DepTuple tuple = accessor.rdp(DepTuple.createTuple(NodeType.PRIVATE_NAMESPACE.getAsString(), "*", "*"));
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation rdp at DepSpace", "azul");
			Printer.println("  -> Operation rdp took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incRdp(System.currentTimeMillis()-time);
			if(tuple == null)
				throw new DirectoryServiceException("Tuple not Found!");

			return new PrivateNameSpaceStats((String)tuple.getFields()[1], (SecretKey)tuple.getFields()[2]);
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
	}

	@Override
	public void putPrivateNameSpaceMetadata(int clientId, PrivateNameSpaceStats pnsStats) throws DirectoryServiceException {
		try {
			long time = System.currentTimeMillis();
			Printer.println("  -> Start operation cas at DepSpace", "azul");
			DepTuple tuple = accessor.cas(DepTuple.createTuple(NodeType.PRIVATE_NAMESPACE.getAsString(), "*", "*"), DepTuple.createAccessControledTuple(new int []{clientId}, new int []{clientId}, NodeType.PRIVATE_NAMESPACE.getAsString(), pnsStats.getIdPath(), pnsStats.getKey()));
			long tempo = System.currentTimeMillis()-time;
			Printer.println("  -> End operation cas at DepSpace", "azul");
			Printer.println("  -> Operation cas took: " + Long.toString(tempo) + " milis", "azul");
			Statistics.incCas(System.currentTimeMillis()-time);
			if(tuple != null)
				throw new DirectoryServiceException("Node already exists");
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
	}

	@Override
	public void putCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException {
		try {
			String s = new String(""+clientId);
			String listStr = listToString(list);
			
			accessor.cas(DepTuple.createTuple(s, "*"), DepTuple.createAccessControledTuple(null, new int[] {clientId}, s, listStr));
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
	}

	@Override
	public List<String[]> getCredentials(int clientId) throws DirectoryServiceException {
		try {
			String s = new String(""+clientId);
			DepTuple tuple = accessor.rdp(DepTuple.createTuple(s, "*"));
			if(tuple == null)
				throw new DirectoryServiceException("There are no client credentials.");
			
			return stringToList((String) tuple.getFields()[1]);
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
		}
	}

	@Override
	public void updateCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException {
		try {
			String s = new String(""+clientId);
			String listStr = listToString(list);
			accessor.replace(DepTuple.createTuple(s, "*"),  DepTuple.createAccessControledTuple(null, new int[]{clientId}, s, listStr ));
		} catch (DepSpaceException e) {
			throw new DirectoryServiceConnectionProblemException("Connection Problem");
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
	
}
