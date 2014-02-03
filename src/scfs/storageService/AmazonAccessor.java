package scfs.storageService;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import javax.crypto.SecretKey;

import scfs.general.Printer;
import scfs.general.Statistics;
import util.Pair;
import amazon.AmazonS3Driver;
import depskyDep.StorageCloudException;

public class AmazonAccessor implements IAccessor {

	private AmazonS3Driver driver;

	public AmazonAccessor(int clientId, String accessKey, String secretKey){


		this.driver = new AmazonS3Driver("cloud1", accessKey, secretKey);
		try {
			driver.initSession();
		} catch (StorageCloudException e) {
			e.printStackTrace();
		}

	}

	@Override
	public byte[] readMatchingFrom(String fileId, SecretKey key, byte[] hash){

		try {
			String name = fileId.concat(getHexString(hash));
			Printer.println("  -> Start download from AmazonS3", "verde");
			long acMil = System.currentTimeMillis();
			byte[] value = driver.downloadData(null, fileId, name);
			long tempo = System.currentTimeMillis() - acMil;
			Statistics.incRead(tempo, value.length);
			Printer.println("  -> End download from AmazonS3", "verde");
			Printer.println("  -> Download operation took: " + Long.toString(tempo) + " milis", "verde");

			if(getHexString(hash).equals(getHexString(generateSHA1Hash(value))))
				return value;
		} catch (Exception e) {
			Printer.println("Read Error", "verde");
		}

		return null;
	}

	@Override
	public byte[] readFrom(String fileId, SecretKey key) {
		try {
			Printer.println("  -> Start download from AmazonS3", "verde");
			long acMil = System.currentTimeMillis();
			byte[] value = driver.downloadData(null, fileId, fileId);
			long tempo = System.currentTimeMillis() - acMil;
			Statistics.incRead(tempo, value.length);
			Printer.println("  -> End download from AmazonS3", "verde");
			Printer.println("  -> Download operation took: " + Long.toString(tempo) + " milis", "verde");

			return value;
		} catch (Exception e) {
			Printer.println("Read Error", "verde");
		}

		return null;
	}

	@Override
	public byte[] writeTo(String fileId, byte[] value, SecretKey key,
			byte[] hash) {
		try {
			String name = fileId.concat(getHexString(hash));
			Printer.println("  -> Start upload to AmazonS3", "verde");
			long acMil = System.currentTimeMillis();
			driver.uploadData(null, fileId, value, name);
			long tempo = System.currentTimeMillis() - acMil;
			Statistics.incWrite(tempo, value.length);
			Printer.println("  -> End upload to AmazonS3 took -> " + tempo, "verde");
			return hash;
		} catch (StorageCloudException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public byte[] writeTo(String fileId, byte[] value, SecretKey key){
		byte[] hash = generateSHA1Hash(value);
		return writeTo(fileId, value, key, hash);
	}

	private byte[] generateSHA1Hash(byte[] data){
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			return sha1.digest(data);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static final byte[] HEX_CHAR_TABLE = {
		(byte) '0', (byte) '1', (byte) '2', (byte) '3',
		(byte) '4', (byte) '5', (byte) '6', (byte) '7',
		(byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
		(byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
	};

	public static String getHexString(byte[] raw)
			throws UnsupportedEncodingException {
		byte[] hex = new byte[2 * raw.length];
		int index = 0;
		for (byte b : raw) {
			int v = b & 0xFF;
			hex[index++] = HEX_CHAR_TABLE[v >>> 4];
			hex[index++] = HEX_CHAR_TABLE[v & 0xF];
		}
		return new String(hex, "ASCII");
	}

	@Override
	public int setPermition(String fileId, String permition,
			LinkedList<Pair<String, String>> cannonicalIds) {

		driver.setAcl(null, fileId, cannonicalIds.get(0).getValue(), permition);
		return 0;
	}

	@Override
	public int delete(String fileId) {
		try {
			driver.deleteData(null, null, fileId);
		} catch (StorageCloudException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public int garbageCollection(String fileId, int numVersionToKeep) {
		//TODO: IMPLEMENT
		return 0;
	}

}
