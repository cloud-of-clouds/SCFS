package scfs.directoryService;

import java.util.Collection;
import java.util.List;

import scfs.directoryService.exceptions.DirectoryServiceConnectionProblemException;
import scfs.directoryService.exceptions.DirectoryServiceException;

public class DirectoryServiceRedirect implements DirectoryService{

	private DirectoryService dis;
	private NoSharingDirectoryService pns;
	private int clientId;

	public DirectoryServiceRedirect(int clientId, DirectoryService dis, NoSharingDirectoryService pns) {
		this.dis = dis;
		this.pns = pns;
		this.clientId=clientId;
	}


	@Override
	public void putMetadata(NodeMetadata metadata) throws DirectoryServiceException {
		if(isPrivate(metadata)){
			pns.putMetadata(metadata);
		}else{
			dis.putMetadata(metadata);
		}
	}

	@Override
	public void updateMetadata(String path, NodeMetadata metadata) throws DirectoryServiceException {
		if(pns.containsMetadata(path)){
			pns.updateMetadata(path, metadata);
		}else{
			dis.updateMetadata(path, metadata);
		}
	}

	@Override
	public void insertMetadataInBuffer(String idPath, NodeMetadata metadata) throws DirectoryServiceException {
		if(pns.getMetadataByIdPath(idPath)!=null){
			pns.insertMetadataInBuffer(idPath, metadata);
		}else{
			dis.insertMetadataInBuffer(idPath, metadata);
		}

	}

	@Override
	public void commitMetadataBuffer(String idPath, byte[] hash) throws DirectoryServiceException {
		if(pns.getMetadataByIdPath(idPath)!=null){
			pns.commitMetadataBuffer(idPath, hash);
		}else{
			dis.commitMetadataBuffer(idPath, hash);
		}
	}

	@Override
	public void removeMetadata(String path) throws DirectoryServiceException {
		if(pns.containsMetadata(path))
			pns.removeMetadata(path);
		else
			dis.removeMetadata(path);
	}

	@Override
	public NodeMetadata getMetadata(String path) throws DirectoryServiceException {
		if(pns.containsMetadata(path))
			return pns.getMetadata(path);
		else
			return dis.getMetadata(path);
	}

	@Override
	public Collection<NodeMetadata> getNodeChildren(String path) throws DirectoryServiceException {
		if(pns.containsMetadata(path))
			return pns.getNodeChildren(path);
		else
			return dis.getNodeChildren(path);
	}

	@Override
	public Collection<NodeMetadata> getAllLinks(String idPath) throws DirectoryServiceException {
		if(pns.getMetadataByIdPath(idPath)!=null)
			return pns.getAllLinks(idPath);
		else
			return dis.getAllLinks(idPath);
	}

	@Override
	public PrivateNameSpaceStats getPrivateNameSpaceMetadata() throws DirectoryServiceException {
		return dis.getPrivateNameSpaceMetadata();
	}

	@Override
	public void putPrivateNameSpaceMetadata(int clientId, PrivateNameSpaceStats pnsStats)
			throws DirectoryServiceException {
		dis.putPrivateNameSpaceMetadata(clientId, pnsStats);
	}

	@Override
	public void putCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException {
		dis.putCredentials(list);
	}

	@Override
	public List<String[]> getCredentials(int clientId) throws DirectoryServiceException {
		return dis.getCredentials(clientId);
	}

	@Override
	public void updateCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException {
		dis.updateCredentials(list);

	}

	private boolean isPrivate(NodeMetadata metadata){
		return metadata.getC_w().length == 1 && metadata.getC_w()[0]==clientId && metadata.getC_r().length==1&&metadata.getC_r()[0]==clientId;

	}


}
