package scfs.lockService;

public abstract class LockService {

	public abstract boolean tryAcquire(String pathId, int time);

	public void acquire(String pathId, int time) {
		while(!tryAcquire(pathId, time))
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { e.printStackTrace();	}
	}

	public abstract void release(String pathId);

	public abstract boolean renew(String pathId, int time);

	public abstract void migrate(String from, String to, int time);
	
	
}
