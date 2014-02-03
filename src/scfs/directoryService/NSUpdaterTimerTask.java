package scfs.directoryService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.TimerTask;

import javax.crypto.SecretKey;

import scfs.general.Printer;
import scfs.storageService.IAccessor;
import scfs.util.ExternalizableHashMap;

public class NSUpdaterTimerTask extends TimerTask{

	private ExternalizableHashMap lastUpdate;
	private boolean isScheduled;
	private IAccessor accessor;
	private SecretKey key;
	private String pathId;
	private ExternalizableHashMap toSend;



	public NSUpdaterTimerTask(IAccessor accessor, SecretKey key, String pathId) {
		this.isScheduled = false;
		this.accessor = accessor;
		this.pathId = pathId;
		this.key = key;
		this.toSend = new ExternalizableHashMap();
	}

	public boolean isScheduled() {
		return isScheduled;
	}

	public void schedule(){
		isScheduled = true;
	}

	public void setLastUpdate(ExternalizableHashMap lastUpdate) {
		synchronized (pathId) {
			this.lastUpdate = lastUpdate;
		}
	}

	@Override
	public void run() {

		while(lastUpdate != null){
			synchronized (pathId) {
				toSend = lastUpdate;
				lastUpdate = null;
			}

			try{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = null;
				try {
					oos = new ObjectOutputStream(baos);
					toSend.writeExternal(oos);
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				byte[] bagArray = baos.toByteArray();
				Printer.println("NS Updater : sending NS.", "azul");
				accessor.writeTo(pathId, bagArray, key, new byte[0]);
			}catch(Exception e){
				e.printStackTrace();
				throw e;
			}
		}
		
		synchronized (pathId) {
			if(lastUpdate==null)
				isScheduled = false;
			else
				this.run();
		}
	}

}
