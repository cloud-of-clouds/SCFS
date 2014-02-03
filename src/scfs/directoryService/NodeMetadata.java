package scfs.directoryService;
import general.DepTuple;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;

import javax.crypto.SecretKey;

import scfs.general.NodeType;

public class NodeMetadata implements Cloneable, Externalizable, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2469002401200857941L;
	private static final int FILE_OR_DIR_INDEX = 0;
	private static final int PARENT_FOLDER_INDEX = 1;
	private static final int NAME_INDEX = 2;
	private static final int STATS_INDEX = 3;
	private static final int ID_PATH_INDEX = 4;
	private static final int ACCESS_KEY_INDEX = 5;

	private NodeType nodeType;
	private String parent;
	private String name;
	private FileStats stats;
	private String id_path;
	private SecretKey key;
	private int[] c_r;
	private int[] c_w;
	private String path;
	private String linkToPath;

	public NodeMetadata() {
	}

	public NodeMetadata(Object[] fields, int[] c_r, int[] c_w) {
		this.nodeType = NodeType.getNodeType((String)fields[FILE_OR_DIR_INDEX]);
		this.parent = (String) fields[PARENT_FOLDER_INDEX];
		this.name = (String) fields[NAME_INDEX];
		this.stats = (FileStats) fields[STATS_INDEX];
		this.id_path = (String) fields[ID_PATH_INDEX];
		if(nodeType==NodeType.SYMLINK){
			this.linkToPath = (String) fields[ID_PATH_INDEX];
		}else{
			this.linkToPath = new String();
		}
		this.key = (SecretKey) fields[ACCESS_KEY_INDEX];
		this.setC_r(c_r);
		this.setC_w(c_w);
		this.path = genPath();
	}

	public NodeMetadata(NodeType nodeType, String parent, String name,
			FileStats stats, String idPath, SecretKey key, int[] c_r, int[] c_w) {
		this.nodeType = nodeType;
		this.parent = parent;
		this.name = name;
		this.stats = stats;
		this.id_path = idPath;
		if(nodeType==NodeType.SYMLINK){
			this.linkToPath = idPath;
		}else{
			this.linkToPath = new String();
		}
		this.key = key;
		this.key = key;
		this.setC_r(c_r);
		this.setC_w(c_w);
		this.path = genPath();
	}

	public String getLinkToPath() {
		return linkToPath;
	}

	public String setLinkToPath() {
		return linkToPath;
	}

	public boolean isDirectory() {
		return nodeType.equals(NodeType.DIR);
	}

	public boolean isFile() {
		return nodeType.equals(NodeType.FILE);
	}

	public boolean isSymLink() {
		return nodeType.equals(NodeType.SYMLINK);
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
		this.path = genPath();
	}

	public String getName() {
		return name;
	}

	public NodeType getNodeType() {
		return nodeType;
	}

	public void setName(String name) {
		this.name = name;
		this.path = genPath();
	}

	public FileStats getStats() {
		return stats;
	}

	public void setStats(FileStats stats) {
		this.stats = stats;
	}

	public void setNodeType(NodeType nodeType) {
		this.nodeType = nodeType;
	}

	public String getId_path() {
		return id_path;
	}

	public void setId_path(String id_path) {
		this.id_path = id_path;
	}

	public SecretKey getKey() {
		return key;
	}

	public void setKey(SecretKey key) {
		this.key = key;
	}

	public void setC_r(int[] c_r) {
		this.c_r = c_r;
	}

	public int[] getC_r() {
		return c_r;
	}

	public void setC_w(int[] c_w) {
		this.c_w = c_w;
	}

	public int[] getC_w() {
		return c_w;
	}

	public DepTuple getAsTuple(){
		if(nodeType == NodeType.SYMLINK)
			return DepTuple.createAccessControledTuple(c_r, c_w, nodeType.getAsString(), parent, name, stats, linkToPath, key);
		else
			return DepTuple.createAccessControledTuple(c_r, c_w, nodeType.getAsString(), parent, name, stats, id_path, key);
	}

	public String getPath(){
		return genPath();

	}

	private String genPath(){
		if(name.equals(""))
			return parent;
		if(parent.equals("/"))
			return parent.concat(name);

		return parent.concat("/").concat(name);
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(c_r);
		result = prime * result + Arrays.hashCode(c_w);
		result = prime * result + ((id_path == null) ? 0 : id_path.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((linkToPath == null) ? 0 : linkToPath.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((nodeType == null) ? 0 : nodeType.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((stats == null) ? 0 : stats.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeMetadata other = (NodeMetadata) obj;
		if (!Arrays.equals(c_r, other.c_r))
			return false;
		if (!Arrays.equals(c_w, other.c_w))
			return false;
		if (id_path == null) {
			if (other.id_path != null)
				return false;
		} else if (!id_path.equals(other.id_path))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (linkToPath == null) {
			if (other.linkToPath != null)
				return false;
		} else if (!linkToPath.equals(other.linkToPath))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (nodeType != other.nodeType)
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (stats == null) {
			if (other.stats != null)
				return false;
		} else if (!stats.equals(other.stats))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if(nodeType == NodeType.SYMLINK)
			return "[ " + nodeType.getAsString() + ", from : " + path + ", to : " + linkToPath +" ]";
		else
			return "[ " + nodeType.getAsString() + ", " + path + ", id=" + id_path + ", " + stats + " ]";
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		NodeMetadata m = new NodeMetadata();

		m.c_r = c_r==null ? null : Arrays.copyOf(c_r, c_r.length);
		m.c_w = c_w==null ? null : Arrays.copyOf(c_w, c_w.length);
		m.id_path = new String(id_path);
		m.nodeType = nodeType;
		m.key = key;
		m.name = new String(name);
		m.parent = new String(parent);
		m.path = new String(path);
		m.stats = (FileStats) stats.clone();
		m.linkToPath = new String(linkToPath);
		
		return m;
	}


	@Override
	public void writeExternal(ObjectOutput out) throws IOException {

		
		out.writeUTF(nodeType.getAsString()); //nodeType
		out.writeUTF(parent); //parent
		out.writeUTF(name); //name

		stats.writeExternal(out); //stats

		out.writeUTF(id_path); //id_path
		out.writeObject(key); //key

		//c_r
		if(c_r != null){
			out.writeInt(c_r.length);
			for(int i : c_r){
				out.writeInt(i);
			}
		}else{
			out.writeInt(-1);
		}

		//c_w
		if(c_w!=null){
			out.writeInt(c_w.length);
			for(int i : c_w){
				out.writeInt(i);
			}
		}else{
			out.writeInt(-1);
		}

		out.writeUTF(path); //path
		out.writeUTF(linkToPath); //linkToPath
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		nodeType = NodeType.getNodeType(in.readUTF()); //nodeType;
		parent = in.readUTF(); //parent;
		name = in.readUTF();//name;

		stats = new FileStats();
		stats.readExternal(in); //stats;

		id_path = in.readUTF(); //id_path;
		key = (SecretKey) in.readObject();//key;
		
		//c_r;
		int len = in.readInt();
		if(len == -1){
			c_r=null;
		}else{
			c_r = new int[len];
			for(int i = 0 ; i<len;i++){
				c_r[i] = in.readInt();
			}
		}

		//c_w;
		len = in.readInt();
		if(len == -1){
			c_w = null;
		}else{
			c_w = new int[len];
			for(int i = 0 ; i<len;i++){
				c_w[i] = in.readInt();
			}
		}
		
		path = in.readUTF(); //path;
		linkToPath = in.readUTF(); //linkToPath;
	}

}