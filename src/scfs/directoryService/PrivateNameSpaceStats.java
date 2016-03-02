package scfs.directoryService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.SecretKey;

public class PrivateNameSpaceStats {

	private String idPath;
	private SecretKey key;

	public PrivateNameSpaceStats(String idPath, SecretKey key) {
		this.idPath = idPath;
		this.key = key;
	}

	public String getIdPath() {
		return idPath;
	}

	public SecretKey getKey() {
		return key;
	}

	public byte[] getBytes() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);

			oos.writeUTF(idPath);
			oos.writeObject(key);

			oos.close();
			baos.close();
			
			return baos.toByteArray();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static PrivateNameSpaceStats getFromBytes(byte[] data) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ObjectInputStream ois = new ObjectInputStream(bais);

			String idPath = ois.readUTF();
			SecretKey key = (SecretKey) ois.readObject();

			bais.close();
			ois.close();
			
			return new PrivateNameSpaceStats(idPath, key);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}


}
