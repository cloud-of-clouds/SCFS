package scfs.directoryService;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class FileStats implements  Serializable, Externalizable, Cloneable{

	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -711199492022722526L;
	private long inode;
	private int mode;
	private int nlink;
	private int uid;
	private int gid; 
	private int rdev;
	private long size; 
	private long blocks;
	private int atime;
	private int mtime;
	private int ctime;
	private byte [] dataHash;
	private HashMap<String, byte[]> xttr;
	private boolean isPrivate;
	private boolean pending;

	public FileStats(long inode, int mode, int nlink, int uid, int gid,
			int rdev, long size, long blocks, int atime, int mtime, int ctime, byte[] dataHash, boolean isPending, boolean isPrivate) {
		this.inode = inode;
		this.mode = mode;
		this.nlink = nlink;
		this.uid = uid;
		this.gid = gid;
		this.rdev = rdev;
		this.size = size;
		this.blocks = blocks;
		this.atime = atime;
		this.mtime = mtime;
		this.ctime = ctime;
		this.pending = isPending;
		this.dataHash = dataHash;
		this.xttr = new HashMap<String, byte[]>();
		this.isPrivate = isPrivate;
	}
	
	public FileStats() {
	}


	public Map<String, byte[]> getXattr() {
		return xttr;
	}

	public long getInode() {
		return inode;
	}

	public void setInode(long inode) {
		this.inode = inode;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public int getNlink() {
		return nlink;
	}

	public void setNlink(int nlink) {
		this.nlink = nlink;
	}

	public boolean isPending(){
		return pending;
	}

	public void setPending(boolean isPending){
		this.pending = isPending;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public int getGid() {
		return gid;
	}

	public void setGid(int gid) {
		this.gid = gid;
	}

	public int getRdev() {
		return rdev;
	}

	public void setRdev(int rdev) {
		this.rdev = rdev;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getBlocks() {
		return blocks;
	}

	public void setBlocks(long blocks) {
		this.blocks = blocks;
	}

	public int getAtime() {
		return atime;
	}

	public void setAtime(int atime) {
		this.atime = atime;
	}

	public void setMtime(int mtime) {
		this.mtime = mtime;
	}

	public int getMtime() {
		return mtime;
	}

	public void setCtime(int ctime) {
		this.ctime = ctime;
	}

	public int getCtime() {
		return ctime;
	}

	public byte[] getDataHash(){
		return dataHash;
	}

	public void setDataHash(byte[] hash){
		dataHash = hash;
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public void setPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + atime;
		result = prime * result + (int) (blocks ^ (blocks >>> 32));
		result = prime * result + ctime;
		result = prime * result + Arrays.hashCode(dataHash);
		result = prime * result + gid;
		result = prime * result + (int) (inode ^ (inode >>> 32));
		result = prime * result + (isPrivate ? 1231 : 1237);
		result = prime * result + mode;
		result = prime * result + mtime;
		result = prime * result + nlink;
		result = prime * result + (pending ? 1231 : 1237);
		result = prime * result + rdev;
		result = prime * result + (int) (size ^ (size >>> 32));
		result = prime * result + uid;
		result = prime * result + ((xttr == null) ? 0 : xttr.hashCode());
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
		FileStats other = (FileStats) obj;
		if (atime != other.atime)
			return false;
		if (blocks != other.blocks)
			return false;
		if (ctime != other.ctime)
			return false;
		if (!Arrays.equals(dataHash, other.dataHash))
			return false;
		if (gid != other.gid)
			return false;
		if (inode != other.inode)
			return false;
		if (mode != other.mode)
			return false;
		if (mtime != other.mtime)
			return false;
		if (nlink != other.nlink)
			return false;
		if (pending != other.pending)
			return false;
		if (rdev != other.rdev)
			return false;
		if (size != other.size)
			return false;
		if(isPrivate!=other.isPrivate)
			return false;
		if (uid != other.uid)
			return false;
		if (xttr == null) {
			if (other.xttr != null)
				return false;
		} else if(xttr.size() != other.xttr.size())
			return false;

		for(String k : xttr.keySet()){
			if(!other.xttr.containsKey(k))
				return false;
			if(!Arrays.equals(xttr.get(k), other.xttr.get(k)))
				return false;
		}

		return true;
	}


	@Override
	public String toString() {
		return "size:"+ size + ", isPending:" + isPending() + ", hash=" + dataHash;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new FileStats(inode, mode, nlink, uid, gid, rdev, size, blocks, atime, mtime, ctime, dataHash!=null ? Arrays.copyOf(dataHash, dataHash.length) : null, pending, isPrivate);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(inode); //inode;
		out.writeInt(mode); //mode;
		out.writeInt(nlink);//nlink;
		out.writeInt(uid);//uid;
		out.writeInt(gid);//gid; 
		out.writeInt(rdev);//rdev;
		out.writeLong(size);//size; 
		out.writeLong(blocks);//blocks;
		out.writeInt(atime);//atime;
		out.writeInt(mtime);//mtime;
		out.writeInt(ctime);//ctime;

		//dataHash;
		out.writeInt(dataHash.length);
		for(int i = 0 ; i<dataHash.length ; i++)
			out.write(dataHash[i]);
		
		//xttr;
		writeExtrXttr(out);

		out.writeBoolean(isPrivate); //isPrivate;
		out.writeBoolean(pending); //pending;

	}


	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		inode = in.readLong(); //inode;
		mode = in.readInt(); //mode;
		nlink = in.readInt();//nlink;
		uid = in.readInt();//uid;
		gid = in.readInt();//gid; 
		rdev = in.readInt();//rdev;
		size = in.readLong();//size; 

		blocks = in.readLong();//blocks;
		atime = in.readInt();//atime;
		mtime = in.readInt();//mtime;
		ctime = in.readInt();//ctime;

		//dataHash;
		int len = in.readInt();
		dataHash = new byte[len];
		for(int i = 0 ; i < len ; i++)
			dataHash[i]=in.readByte();
		
		

		//xttr;
		readExtrXttr(in);

		isPrivate = in.readBoolean(); //isPrivate;
		pending = in.readBoolean(); //pending;

	}
	
	private void writeExtrXttr(ObjectOutput out) throws IOException{
		out.writeInt(xttr.size());
		for(Entry<String, byte[]> e : xttr.entrySet()){
			out.writeUTF(e.getKey());
			out.writeInt(e.getValue().length);
			out.write(e.getValue());
		}
	}

	private void readExtrXttr(ObjectInput in) throws IOException{
		xttr = new HashMap<String, byte[]>();
		int len = in.readInt();
		for(int i = 0 ; i<len;i++){
			String key = in.readUTF();
			int lenValue = in.readInt();
			byte[] value = new byte[lenValue];
			in.read(value);
			xttr.put(key, value);
		}
	}

}