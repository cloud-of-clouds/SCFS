package scfs.lockService;
import general.DepSpaceException;
import general.DepTuple;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import scfs.general.SCFSConstants;
import scfs.general.Printer;
import scfs.general.Statistics;
import client.DepSpaceAccessor;


public class LockService {


	private DepSpaceAccessor accessor;
	private int clientId;
	private Map<String, Integer> lockedFiles;
	private Map<String, LockRenewer> renewerTasks;

	public LockService(DepSpaceAccessor accessor, int clientId) {
		this.accessor = accessor;
		this.clientId = clientId;
		this.lockedFiles = new ConcurrentHashMap<String, Integer>();
		this.renewerTasks = new ConcurrentHashMap<String, LockRenewer>();
	}

	public boolean tryAcquire(String pathId, int time) {
		if(accessor==null)
			return true;
		
		if(lockedFiles.containsKey(pathId)){
			lockedFiles.put(pathId, lockedFiles.get(pathId)+1);
			return true;
		}else{
			try {
				long t = System.currentTimeMillis();
				Statistics.incOpen();
				Printer.println("  -> Start operation cas at DepSpace", "azul");
				if(accessor.cas(DepTuple.createTuple(SCFSConstants.LOCK_PREFIX,pathId), DepTuple.createAccessControledTimedTuple(null, new int[] {clientId}, time, SCFSConstants.LOCK_PREFIX,pathId)) == null){
					long tempo = System.currentTimeMillis()-t;
					Printer.println("  -> End operation cas at DepSpace", "azul");
					Printer.println("  -> Operation cas took: " + Long.toString(tempo) + " milis", "azul");
					lockedFiles.put(pathId, 1);
					LockRenewer renewTask = new LockRenewer(time, this, pathId);
					renewerTasks.put(pathId, renewTask);
					renewTask.start();
					return true;
				}
			} catch (DepSpaceException e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}

	public void acquire(String pathId, int time) {
		while(!tryAcquire(pathId, time))
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { e.printStackTrace();	}
	}

	public void release(String pathId) {
		if(accessor==null)
			return;
		
		if(lockedFiles.containsKey(pathId)){
			int nLocks = lockedFiles.get(pathId);
			if(nLocks==1){
				try {
					long time = System.currentTimeMillis();
					Printer.println("  -> Start operation inp at DepSpace", "azul");
					Statistics.incClose();
					accessor.inp(DepTuple.createTuple(SCFSConstants.LOCK_PREFIX, pathId));
					long tempo = System.currentTimeMillis()-time;
					Printer.println("  -> End operation inp at DepSpace", "azul");
					Printer.println("  -> Operation inp took: " + Long.toString(tempo) + " milis", "azul");
				} catch (DepSpaceException e) {
					e.printStackTrace();
				}
				lockedFiles.remove(pathId);
				renewerTasks.remove(pathId).exit();
			}else
				lockedFiles.put(pathId, nLocks-1);
		}

	}

	public boolean renew(String pathId, int time){
		//Printer.println("-------> RENEW CALLED!");
		if(accessor==null)
			return true;
		
		try {
			return accessor.renew(DepTuple.createTuple(SCFSConstants.LOCK_PREFIX,pathId), time) != null;
		} catch (DepSpaceException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void migrate(String from, String to, int time) {
		if(accessor==null)
			return;
		
		try {
			if(lockedFiles.containsKey(from)){
				if(!lockedFiles.containsKey(to)){
					accessor.cas(DepTuple.createTuple(SCFSConstants.LOCK_PREFIX, to), DepTuple.createAccessControledTimedTuple(null, new int[] {clientId}, time, SCFSConstants.LOCK_PREFIX,to));
					lockedFiles.put(to, lockedFiles.get(from));
					LockRenewer renewTask = new LockRenewer(time, this, to);
					renewerTasks.put(to, renewTask);
					renewTask.start();
				}else{
					lockedFiles.put(to, lockedFiles.get(from)+lockedFiles.get(to));
				}
				accessor.inp(DepTuple.createTuple(SCFSConstants.LOCK_PREFIX, from));
				renewerTasks.remove(from).exit();
			}

		} catch (DepSpaceException e) {
			e.printStackTrace();
		}
	}

}
