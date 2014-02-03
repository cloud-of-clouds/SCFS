package scfs.general;


public class Statistics {

	public static int open;
	public static int close;
	
	public static int rdp;
	public static double timeRdp;
	public static int getMeta;

	public static int cas;
	public static double timeCas;
	public static int putMeta;

	public static int inp;
	public static double timeInp;
	public static int delMeta;

	public static int replace;
	public static double timeReplace;
	public static int updateMeta;

	public static int rdall;
	public static long timeRdall;
	public static int getAllLinksMeta;
	public static int getDirMeta;

	public static int readInMem;
	public static long timeReadInMem;
	public static long numBytesReadInMem;
	
	public static int readInDisk;
	public static long timeReadInDik;
	public static long numBytesReadInDisk;
	
	public static int read;
	public static long timeRead;
	public static long numBytesRead;

	public static int writeInMem;
	public static long timeWriteInMem;
	public static long numBytesWriteInMem;
	
	public static int writeInDisk;
	public static long timeWriteInDisk;
	public static long numBytesWriteInDisk;

	
	public static int write;
	public static long timeWrite;
	public static long numBytesWrite;


	public static int delete;
	public static long timeDelete;

	public static boolean reseted;

	public static long initPoint;
	public static long endPoint;
	private static long timeGetMeta;
	private static long timePutMeta;
	private static long timeDelMeta;
	private static long timeUpdateMeta;
	private static long timeGetDirMeta;
	private static long timeGetLinksMeta;


	public static void init(){
		System.out.println("»»» Init called ...");
		rdp=cas=inp=replace=rdall=read=write=delete=getMeta=putMeta=delMeta=updateMeta=getAllLinksMeta=getDirMeta=open=close=0;
		timeRdp=timeCas=timeInp=timeReplace=timeRdall=timeRead=timeWrite=timeDelete=timeGetMeta=timePutMeta=timeDelMeta=timeUpdateMeta=timeGetDirMeta=timeGetLinksMeta =0;
		
		numBytesRead=numBytesReadInDisk=numBytesReadInMem=numBytesWrite=numBytesWriteInDisk=numBytesWriteInMem=0;
		
		reseted = false;
		initPoint = System.currentTimeMillis();
		endPoint=initPoint;
	}


	public static void incGetMeta(long time){
		if(reseted)
			init();
		getMeta++;
		timeGetMeta += time;
		
		endPoint = System.currentTimeMillis();
	}

	public static void incPutMeta(long time){
		if(reseted)
			init();
		putMeta++;
		timePutMeta += time;
		endPoint = System.currentTimeMillis();
	}

	public static void incDelMeta(long time){
		if(reseted)
			init();
		delMeta++;
		timeDelMeta += time;
		endPoint = System.currentTimeMillis();
	}

	public static void incUpdateMeta(long time){
		if(reseted)
			init();
		updateMeta++;
		timeUpdateMeta += time;
		endPoint = System.currentTimeMillis();
	}

	public static void incGetDirMeta(long time){
		if(reseted)
			init();
		getDirMeta++;
		timeGetDirMeta += time;
		endPoint = System.currentTimeMillis();
	}

	public static void incGetAllLinksMeta(long time){
		if(reseted)
			init();
		getAllLinksMeta++;
		timeGetLinksMeta += time;
		endPoint = System.currentTimeMillis();
	}

	public static void incRdp(long time){
		if(reseted)
			init();
		rdp++;
		timeRdp += time;
		endPoint = System.currentTimeMillis();
	}

	public static void incCas(long time){
		if(reseted)
			init();
		cas++;
		timeCas += time;
		endPoint = System.currentTimeMillis();
	}

	public static void incInp(long time){
		if(reseted)
			init();
		inp++;
		timeInp += time;
		endPoint = System.currentTimeMillis();
	}

	public static void incReplace(long time){
		if(reseted)
			init();
		replace++;
		timeReplace += time;
		endPoint = System.currentTimeMillis();
	}

	public static void incRdall(long time) {
		if(reseted)
			init();
		rdall++;
		timeRdall += time;
		endPoint = System.currentTimeMillis();
	}

	public static void incRead(long time, int numBytes){
		if(reseted)
			init();
		read++;
		timeRead += time;
		numBytesRead += numBytes;
		endPoint = System.currentTimeMillis();
	}
	
	public static void incReadInDisk(long time, int numBytes){
		if(reseted)
			init();
		readInDisk++;
		timeReadInDik += time;
		numBytesReadInDisk += numBytes;
		endPoint = System.currentTimeMillis();
	}
	
	public static void incReadInMem(long time, int numBytes){
		if(reseted)
			init();
		readInMem++;
		timeReadInMem += time;
		numBytesReadInMem += numBytes;
		endPoint = System.currentTimeMillis();
	}
	
	public static void incOpen(){
		if(reseted)
			init();
		open++;
		endPoint = System.currentTimeMillis();
	}
	public static void incClose(){
		if(reseted)
			init();
		close++;
		endPoint = System.currentTimeMillis();
	}

	public static void incWrite(long time, int numBytes){
		if(reseted)
			init();
		write++;
		timeWrite += time;
		numBytesWrite+=numBytes;
		endPoint = System.currentTimeMillis();
	}
	
	public static void incWriteInDisk(long time, int numBytes){
		if(reseted)
			init();
		writeInDisk++;
		timeWriteInDisk+=time;
		numBytesWriteInDisk+=numBytes;
	}
	
	public static void incWriteInMem(long time, int numBytes){
		if(reseted)
			init();
		writeInMem++;
		timeWriteInMem+=time;
		numBytesWriteInMem+=numBytes;
	}

	public static void incDelete(long time){
		if(reseted)
			init();
		delete++;
		timeDelete += time;
		endPoint = System.currentTimeMillis();
	}

	public static void reset(){
		reseted=true;
	}

	public static String getReport(){
		String res = "start timestamp: " + initPoint + "\nend timestamp: " + endPoint;
		res = res.concat("\n\t--\n");
		res = res.concat("DirectoryService:\n");

		res = res.concat("\tgetMeta: " + getMeta + "\t" + timeGetMeta/(getMeta>0?getMeta:1) + " ms");
		res = res.concat("\n\tputMeta: " + putMeta + "\t" + timePutMeta/(putMeta>0?putMeta:1) + " ms");
		res = res.concat("\n\tdelMeta: " + delMeta+ "\t" + timeDelMeta/(delMeta>0?delMeta:1) + " ms");
		res = res.concat("\n\tupdateMeta: " + updateMeta+ "\t" + timeUpdateMeta/(updateMeta>0?updateMeta:1) + " ms");
		res = res.concat("\n\tgetAllLinksMeta: " + getAllLinksMeta+ "\t" + timeGetLinksMeta/(getAllLinksMeta>0?getAllLinksMeta:1) + " ms");
		res = res.concat("\n\tgetDirMeta: " + getDirMeta+ "\t" + timeGetDirMeta/(getDirMeta>0?getDirMeta:1) + " ms");

		res = res.concat("\n\n\t--\n");
		res = res.concat("DepSpace:\n");

		res = res.concat("\trdp: " + rdp + "\t" + timeRdp/(rdp>0?rdp:1) + " ms\n");
		res = res.concat("\tinp: " + inp + "\t" + timeInp/(inp>0?inp:1) + " ms\n");
		res = res.concat("\tcas: " + cas + "\t" + timeCas/(cas>0?cas:1) + " ms\n");
		res = res.concat("\treplace: " + replace + "\t" + timeReplace/(replace>0?replace:1) + " ms\n");
		res = res.concat("\trdall: " + rdall + "\t" + timeRdall/(rdall>0?rdall:1) + " ms\n");

		res = res.concat("\t--\n");
		res = res.concat("DepSky:\n");
		res = res.concat("\tread: " + read + "(" + numBytesRead + ")\t" + timeRead/(read>0?read:1) + " ms\n");
		res = res.concat("\twrite: " + write + "(" + numBytesWrite + ")\t" + timeWrite/(write>0?write:1) + " ms\n");
//		res = res.concat("\tdelete: " + delete + "\t" + timeDelete/(delete>0?delete:1) + " ms\n");
		res = res.concat("\t--\n");
		res = res.concat("Disk:\n");
		res = res.concat("\tread: " + readInDisk + "(" + numBytesReadInDisk + ")\t" + timeReadInDik/(readInDisk>0?readInDisk:1) + " nanos\n");
		res = res.concat("\twrite: " + writeInDisk + "(" + numBytesWriteInDisk + ")\t" + timeWriteInDisk/(writeInDisk>0?writeInDisk:1) + " nanos\n");
		
		res = res.concat("\t--\n");
		res = res.concat("Memory:\n");
		res = res.concat("\tread: " + readInMem + "(" + numBytesReadInMem + ")\t" + timeReadInMem/(readInMem>0?readInMem:1) + " nanos\n");
		res = res.concat("\twrite: " + writeInMem + "(" + numBytesWriteInMem + ")\t" + timeWriteInMem/(writeInMem>0?writeInMem:1) + " nanos\n");
		
		res = res.concat("\t--\n");
		res = res.concat("Lock Service:\n");
		res = res.concat("\ttryAquire: " + open + "\n");
		res = res.concat("\trelease: " + close + "\n");

		return res;	
	}

}
