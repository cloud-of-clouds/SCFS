package scfs.general;

public class EntryStatsDas {
	
	private int numBytes;
	private int time;
	
	public EntryStatsDas(int numBytes, int time){
		this.numBytes = numBytes;
		this.time = time;
	}
	
	public int getNumBytes(){
		return numBytes;
	}
	public int getTime(){
		return time;
	}

}
