package scfs.general;

public enum NodeType {
	FILE, DIR, SYMLINK, PRIVATE_NAMESPACE;
	
	public String getAsString() {
		return this.name();
	}
	
	public static NodeType getNodeType(String name){
		if(name.equalsIgnoreCase("FILE"))
			return FILE;
		if(name.equalsIgnoreCase("DIR"))
			return DIR;
		if(name.equalsIgnoreCase("SYMLINK"))
			return SYMLINK;
		if(name.equalsIgnoreCase("PRIVATE_NAMESPACE"))
			return PRIVATE_NAMESPACE;
		return null;
	}
}
