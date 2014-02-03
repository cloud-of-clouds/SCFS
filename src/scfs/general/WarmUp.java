package scfs.general;

import scfs.directoryService.DirectoryService;
import scfs.directoryService.NodeMetadata;
import scfs.directoryService.exceptions.DirectoryServiceException;
import depskys.core.MyAESCipher;

public class WarmUp {

	private DirectoryService dis;

	public WarmUp(DirectoryService dis) {
		this.dis = dis;
	}

	public void run(){
		for(int i=0 ; i<2000 ; i++){
			try{
				dis.putMetadata(new NodeMetadata(NodeType.DIR, "/", i+"", SCFS.createDefaultFileStats(NodeType.DIR, 0, 0), "", MyAESCipher.generateSecretKey(), new int[]{4}, new int [] {4}));
			}catch (DirectoryServiceException e){
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for(int i=0 ; i<2000 ; i++){
			try{
				dis.updateMetadata("/"+i, new NodeMetadata(NodeType.DIR, "/", i+"", SCFS.createDefaultFileStats(NodeType.DIR, 0, 0), "", MyAESCipher.generateSecretKey(), new int[]{4}, new int [] {4}));

			}catch (DirectoryServiceException e){
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for(int i=0 ; i<2000 ; i++){
			try{
				dis.getNodeChildren("/"+i);

			}catch (DirectoryServiceException e){
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		for(int i=0 ; i<2000 ; i++){
			try{
				dis.getMetadata("/" + (i%1000));

			}catch (DirectoryServiceException e){
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for(int i=0 ; i<2000 ; i++){
			try{
				dis.removeMetadata("/"+i);


			}catch (DirectoryServiceException e){
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
