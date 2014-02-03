package scfs.lockService;


public class LockRenewer extends Thread {

	private boolean stop;
	private int time;
	private LockService lockService;
	private String pathId;
	private int percent;

	public LockRenewer(int time, LockService lockService, String pathId) {
		this.stop = false;
		this.time = time;
		this.lockService = lockService;
		this.pathId = pathId;
		this.percent = (int) (time * 0.1);
	}

	@Override
	public void run() {
		boolean exception = false;
		int renewError = 0;
		while(!stop){
			try {
				if(!exception && renewError == 0)
					Thread.sleep(time-percent);

				if(!stop && lockService.renew(pathId, time)){
					renewError = 0;
				}else{
					renewError ++;
					if(renewError == 4)
						stop=true;
				}
				exception = false;
			} catch (InterruptedException e) {
				exception = true;
			} 
		}
	}

	public void exit() {
		stop=true;
	}

}
