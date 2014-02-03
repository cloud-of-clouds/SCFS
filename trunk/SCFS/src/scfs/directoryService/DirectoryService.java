package scfs.directoryService;

import java.util.Collection;
import java.util.List;

import scfs.directoryService.exceptions.DirectoryServiceConnectionProblemException;
import scfs.directoryService.exceptions.DirectoryServiceException;



public interface DirectoryService {

	public void putMetadata(NodeMetadata metadata) throws DirectoryServiceException;
	
	public void updateMetadata(String path, NodeMetadata metadata) throws DirectoryServiceException;
	
	public void insertMetadataInBuffer(String path, NodeMetadata metadata) throws DirectoryServiceException;
	
	public void commitMetadataBuffer(String path, byte[] hash) throws DirectoryServiceException;

	public void removeMetadata(String path) throws DirectoryServiceException;
	
	public NodeMetadata getMetadata(String path) throws DirectoryServiceException;
	
	public Collection<NodeMetadata> getNodeChildren(String path) throws DirectoryServiceException;
	
	public Collection<NodeMetadata> getAllLinks(String idPath) throws DirectoryServiceException;
	
	public PrivateNameSpaceStats getPrivateNameSpaceMetadata() throws DirectoryServiceException;
	
	public void putPrivateNameSpaceMetadata(int clientId, PrivateNameSpaceStats pnsStats) throws DirectoryServiceException;
	
	public void putCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException;
	
	public List<String[]> getCredentials(int clientId) throws DirectoryServiceException;
	
	public void updateCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException;
}
