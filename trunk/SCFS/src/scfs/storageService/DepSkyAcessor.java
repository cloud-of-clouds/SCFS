package scfs.storageService;
import java.util.LinkedList;

import javax.crypto.SecretKey;

import scfs.general.Printer;
import scfs.general.Statistics;
import util.Pair;
import depskys.core.DepSkySDataUnit;
import depskys.core.LocalDepSkySClient;



public class DepSkyAcessor implements IAccessor{

	private LocalDepSkySClient localDS;

	public DepSkyAcessor(int clientId){
		this.localDS = new LocalDepSkySClient(clientId, true);
	}

	public byte[] readMatchingFrom(String fileId, SecretKey key, byte[] hash){
		DepSkySDataUnit dataU = new DepSkySDataUnit(fileId);
		dataU.clearAllCaches();
		byte[] data = null;
		try {
			Printer.println("  -> Start download at depsky", "verde");
			long acMil = System.currentTimeMillis();
			data = localDS.readMatching(dataU, hash);
			long tempo = System.currentTimeMillis() - acMil;
			Statistics.incRead(tempo, data.length);
			Printer.println("  -> End download at depsky", "verde");
			Printer.println("  -> Download operation took: " + Long.toString(tempo) + " milis", "verde");
		} catch (Exception e) {
			Printer.println("Read Error", "verde");
		}

		return data;
	}
	
	public byte[] readFrom(String fileId, SecretKey key){
		DepSkySDataUnit dataU = new DepSkySDataUnit(fileId);
		dataU.clearAllCaches();
		byte[] data = null;
		try {
			Printer.println("  -> Start download at depsky", "verde");
			long acMil = System.currentTimeMillis();
			data = localDS.read(dataU);
			long tempo = System.currentTimeMillis() - acMil;
			Statistics.incRead(tempo, data.length);
			Printer.println("  -> End download at depsky", "verde");
			Printer.println("  -> Download operation took: " + Long.toString(tempo) + " milis", "verde");
		} catch (Exception e) {
			Printer.println("Read Error", "verde");
		}

		return data;
	}

	@Override
	public byte[] writeTo(String fileId, byte[] value, SecretKey key,
			byte[] hash) {
		return writeTo(fileId, value, key);
	}
	
	public byte[] writeTo(String fileId, byte[] value, SecretKey key){

		try {
			DepSkySDataUnit dataU = new DepSkySDataUnit(fileId);
			dataU.setUsingPVSS(true);
			Printer.println("  -> Start upload at depsky", "verde");
			long acMil = System.currentTimeMillis();
			byte[] hash = localDS.write(dataU, value);
			long tempo = System.currentTimeMillis() - acMil;
			Statistics.incWrite(tempo, value.length);
			Printer.println("  -> End upload at depsky", "verde");
			Printer.println("  -> Upload operation took: " + Long.toString(tempo) + " milis", "verde");
			
			return hash;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	public int delete(String fileId){
		return 0;
	}

	public int garbageCollection(String fileId, int numVersionToKeep){
		return 0;
	}
	
	public int setPermition(String fileId, String permition, LinkedList<Pair<String,String>> cannonicalIds){
		DepSkySDataUnit dataU = new DepSkySDataUnit(fileId);
		dataU.setUsingErsCodes(true);
		try {
			localDS.setAcl(dataU, permition, cannonicalIds);
			return 1;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

}
