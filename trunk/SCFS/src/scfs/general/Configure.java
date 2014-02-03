package scfs.general;

public class Configure {

	// options of C2FS
	private final String optimizedCache = "-optimizedCache";
	private final String printer = "-printer";
	private final String sync = "--sync-to-cloud";
	private final String blockCloud = "--non-blocking-cloud";
	private final String nonSharing = "--non-sharing";
	private final String deltaTag = "--delta";


	private boolean isOptimizedCache;
	private boolean isPrinter;
	private boolean isSync;
	private boolean isNomBlockCloud;
	private boolean isNonSharing;
	private int numOfSendingThreads;
	private int delta;

	private String mount;
	private int clientId;

	public Configure(String mount, int clientId){

		this.mount = mount;
		this.clientId = clientId;

		// default : dont use in main memory data cache 
		this.isOptimizedCache = false;
		// default : dont use prints
		this.isPrinter = false;
		// default : fsync saves data in local disk
		this.isSync = false;
		// default : system blocks while data is sent to the cloud
		this.isNomBlockCloud = false;
		// default : default number of sendind threads is 1
		this.numOfSendingThreads = 1;
		// default : sharing is turned on
		this.isNonSharing = false;

		this.delta = 500;
		
	}

	public boolean setConfiguration(String[] args){
		boolean flag = true;
		for(int i = 2; i < args.length; i++){
			if(args[i].equals(optimizedCache)){
				isOptimizedCache = true;
			}else if(args[i].equals(printer)){
				isPrinter = true;
			}else if(args[i].equals(sync)){
				isSync = true;
			}else if(args[i].equals(blockCloud)){
				isNomBlockCloud = true;
			}else if(args[i].equals(nonSharing)){
				isNonSharing = true;
			}else if(args[i].startsWith(deltaTag)){
				try{
					String value = args[i].split("=")[1];
					delta = Integer.parseInt(value);
				}catch(Exception e){
					System.out.println("Invalid Arg");
					flag = false;
					i = args.length;
				}
			}else{
				System.out.println("Invalid Arg");
				flag = false;
				i = args.length;
			}			
		}		
		return flag;
	}

	public int getClientId(){
		return clientId;
	}

	public String getMount(){
		return mount;
	}

	public boolean getIsOptimizedCache(){
		return isOptimizedCache;
	}

	public boolean getIsPrinter(){
		return isPrinter;
	}

	public boolean getIsSync(){
		return isSync;
	}

	public boolean isNonSharing(){
		return isNonSharing;
	}

	public boolean getIsNomBlockCloud(){
		return isNomBlockCloud;
	}

	public int getNumOfSendingThreads(){
		return numOfSendingThreads;
	}
	
	public int getDelta() {
		return delta;
	}
	
}
