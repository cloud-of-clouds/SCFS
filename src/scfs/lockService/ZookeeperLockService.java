package scfs.lockService;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import scfs.general.Printer;
import scfs.general.Statistics;

public class ZookeeperLockService extends LockService{

	private ZooKeeper zk;
	private ConcurrentHashMap<String, Integer> lockedFiles;

	public ZookeeperLockService(ZooKeeper zk) {
		this.zk = zk;
		this.lockedFiles = new ConcurrentHashMap<String, Integer>();
	}

	@Override
	public boolean tryAcquire(String pathId, int time) {
		if(zk==null)
			return true;

		Printer.println(" ====> LOCK .." + pathId, "roxo");

		final String lockName = "/lock-"+pathId;

		if(lockedFiles.containsKey(pathId)){
			lockedFiles.put(pathId, lockedFiles.get(pathId)+1);
			return true;
		}else{
			try {
				long t = System.currentTimeMillis();
				Statistics.incOpen();
				Printer.println("  -> Start operation insert at Zookeeper", "azul");
				zk.create(lockName, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
				long tempo = System.currentTimeMillis()-t;
				Printer.println("  -> End operation insert at Zookeeper", "azul");
				Printer.println("  -> Operation insert took: " + Long.toString(tempo) + " milis", "azul");
				lockedFiles.put(pathId, 1);
				return true;
			} catch (KeeperException | InterruptedException e) {
				return false;
			}

		}

	}

	@Override
	public void release(String pathId) {
		if(zk == null)
			return;

		Printer.println(" ====> UNLOCK .." + pathId, "roxo");

		final String lockName = "/lock-"+pathId;
		if(lockedFiles.containsKey(pathId)){
			int nLocks = lockedFiles.get(pathId);
			if(nLocks==1){
				long time = System.currentTimeMillis();
				Printer.println("  -> Start operation delete at Zookeeper", "azul");
				Statistics.incClose();

				try {
					zk.delete(lockName, -1);
				} catch (KeeperException | InterruptedException e) {}

				long tempo = System.currentTimeMillis()-time;
				Printer.println("  -> End operation delete at Zookeeper", "azul");
				Printer.println("  -> Operation delete took: " + Long.toString(tempo) + " milis", "azul");

				lockedFiles.remove(pathId);
			}else
				lockedFiles.put(pathId, nLocks-1);
		}

	}

	@Override
	public boolean renew(String pathId, int time) {
		return true;
	}

	@Override
	public void migrate(String from, String to, int time) {
		if(zk == null)
			return;

		//TODO: 
		return;
	}

}
