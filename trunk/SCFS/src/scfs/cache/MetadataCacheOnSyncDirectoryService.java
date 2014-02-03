package scfs.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import scfs.directoryService.NoCacheDirectoryService;
import scfs.directoryService.NodeMetadata;
import scfs.directoryService.PrivateNameSpaceStats;
import scfs.directoryService.exceptions.DirectoryServiceConnectionProblemException;
import scfs.directoryService.exceptions.DirectoryServiceException;
import scfs.general.Statistics;

public class MetadataCacheOnSyncDirectoryService implements MetadataCache{

	public static int DELTA_TIME = 500;

	private Map <String, MetadataCacheEntry> pathToMetadata;
	private Map <String, MetadataUpdaterTimerTask> tasks;
	private NoCacheDirectoryService directoryService;
	private Timer timer;
	private Map<String , NodeMetadata> buffer;

	public MetadataCacheOnSyncDirectoryService(NoCacheDirectoryService directoryService) {
		this.timer = new Timer();
		this.directoryService = directoryService;
		this.pathToMetadata = new ConcurrentHashMap<String, MetadataCacheEntry>();
		this.tasks = new ConcurrentHashMap<String, MetadataUpdaterTimerTask>();
		this.buffer = new ConcurrentHashMap<String, NodeMetadata>();
	}

	@Override
	public void putMetadata(NodeMetadata metadata) throws DirectoryServiceException {
		directoryService.putMetadata(metadata);
		if(DELTA_TIME > 0)
			pathToMetadata.put(metadata.getPath(), new MetadataCacheEntry(metadata, System.currentTimeMillis(), false, -1));
	}


	@Override
	public NodeMetadata getMetadata(String path) throws DirectoryServiceException {

		NodeMetadata metadata = null;		
		if(!inCache(path)){
			metadata = directoryService.getMetadata(path);
		}else{
			if( DELTA_TIME>0 && System.currentTimeMillis() <= (pathToMetadata.get(path).getTime()+DELTA_TIME)){
				metadata = pathToMetadata.get(path).getMetadata();
			}else{
				pathToMetadata.remove(path);
				metadata = directoryService.getMetadata(path);
			}

		}


		if(!inCache(path) && DELTA_TIME>0 ){
			pathToMetadata.put(path, new MetadataCacheEntry(metadata, System.currentTimeMillis(), false, -1));
		}
		NodeMetadata res = null;

		if(buffer.containsKey(metadata.getId_path())){
			try {
				res = (NodeMetadata) metadata.clone();
				res.getStats().setDataHash(buffer.get(metadata.getId_path()).getStats().getDataHash());
				res.getStats().setSize(buffer.get(metadata.getId_path()).getStats().getSize());
				res.getStats().setPending(buffer.get(metadata.getId_path()).getStats().isPending());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}

			return res;
		}else{
			return metadata;
		}

	}


	@Override
	public void updateMetadata(String path, NodeMetadata metadata) throws DirectoryServiceException {
		if(DELTA_TIME==0){
			directoryService.updateMetadata(path, metadata);
			return;
		}

		if(!inCache(path) || (inCache(path) && System.currentTimeMillis() > pathToMetadata.get(path).getTime()+DELTA_TIME)){
			long time = System.currentTimeMillis();
			getMetadata(path); //bring it to cache
			Statistics.incGetMeta(System.currentTimeMillis() - time);
		}

		if(!path.equals(metadata.getPath())){
			if(isUpdateTaskRunnig(path)){
				synchronized (tasks.get(path)) {
					tasks.get(path).setCommit(false);
					removeFromCache(path);
					pathToMetadata.remove(path);
					buffer.remove(metadata.getId_path());
				}
				if(isUpdateTaskRunnig(metadata.getPath())){
					synchronized (tasks.get(metadata.getPath())) {
						if(tasks.containsKey(metadata.getPath()))
							tasks.get(metadata.getPath()).setCommit(false);
					}
				}
				if(inCache(metadata.getPath())){
					pathToMetadata.remove(metadata.getPath());
					removeFromCache(metadata.getPath());
				}
			}
			directoryService.updateMetadata(path, metadata);
		}else{
			pathToMetadata.get(path).setUpdated(metadata);
			if(!isUpdateTaskRunnig(path)){
				tasks.put(path, new MetadataUpdaterTimerTask(path, pathToMetadata.get(path).getMetadata(), directoryService, this));
				long time = DELTA_TIME - (System.currentTimeMillis() - pathToMetadata.get(path).getTime());
				timer.schedule(tasks.get(path),   time < 0 ? 0 : time );

			}
			tasks.get(metadata.getPath()).setCommit(true);

		}

	}

	@Override
	public void removeMetadata(String path) throws DirectoryServiceException {

		if(isUpdateTaskRunnig(path)){
			tasks.get(path).setRemoved(true);
			tasks.remove(path);
		}

		String idPath = null;
		for(NodeMetadata m : buffer.values())
			if(m.getPath().equals(path))
				idPath = m.getId_path();

		if(idPath != null)
			buffer.remove(idPath);
		pathToMetadata.remove(path);
		removeFromCache(path);
		directoryService.removeMetadata(path);
	}


	@Override
	public Collection<NodeMetadata> getNodeChildren(String path) throws DirectoryServiceException {
		Collection<NodeMetadata> res = new ArrayList<NodeMetadata>();

		Collection<NodeMetadata> list = directoryService.getNodeChildren(path);

		for(NodeMetadata m : list){
			boolean flag = false;

			for(MetadataUpdaterTimerTask mtask : tasks.values()){
				if(mtask.getPath().equals(m.getPath())){
					if(mtask.getMetadata().getParent().equals(path))
						res.add(mtask.getMetadata());
					flag=true;
					break;
				}

			}
			if(!flag)
				res.add(m);
		}
		return res;
	}

	@Override
	public Collection<NodeMetadata> getAllLinks(String idPath) throws DirectoryServiceException {

		Collection<NodeMetadata> links = directoryService.getAllLinks(idPath);
		Collection<NodeMetadata> result = new ArrayList<NodeMetadata>();


		for(NodeMetadata m : links){
			if(buffer.containsKey(m.getId_path())){
				try {
					NodeMetadata res = (NodeMetadata) m.clone();
					res.getStats().setDataHash(buffer.get(m.getId_path()).getStats().getDataHash());
					res.getStats().setSize(buffer.get(m.getId_path()).getStats().getSize());
					res.getStats().setPending(buffer.get(m.getId_path()).getStats().isPending());
					result.add(res);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}

			}else{
				result.add(m);
			}
		}
		return result;
	}

	@Override
	public void removeFromCache(String path) {
		pathToMetadata.remove(path);
		if(tasks.get(path)!=null){
			tasks.get(path).setCommit(false);
			tasks.remove(path);
		}
	}


	@Override
	public void insertMetadataInBuffer(String idPath, NodeMetadata metadata) throws DirectoryServiceException {
		buffer.put(idPath, metadata);
	}

	@Override
	public void commitMetadataBuffer(String idPath, byte[] hash) throws DirectoryServiceException {

		NodeMetadata m_buffer =null;
		if(buffer.containsKey(idPath))
			m_buffer = buffer.get(idPath);

		Collection<NodeMetadata> list;
		try{
			list = directoryService.getAllLinks(idPath);
		}catch (DirectoryServiceException e) {
			e.printStackTrace();
			return;
		}
		for(NodeMetadata m_new : list ){
			if(m_buffer != null)
				m_new.getStats().setSize(m_buffer.getStats().getSize());

			m_new.getStats().setDataHash(hash);
			m_new.getStats().setPending(false);
			directoryService.updateMetadata(m_new.getPath(), m_new);
		}
		buffer.remove(idPath);
	}

	private boolean inCache(String path){
		return pathToMetadata.containsKey(path);
	}

	private boolean isUpdateTaskRunnig(String path){
		return tasks.get(path) != null;		
	}

	@Override
	public PrivateNameSpaceStats getPrivateNameSpaceMetadata() throws DirectoryServiceException {
		return directoryService.getPrivateNameSpaceMetadata();
	}

	@Override
	public void putPrivateNameSpaceMetadata(int clientId, PrivateNameSpaceStats pnsStats) throws DirectoryServiceException {
		directoryService.putPrivateNameSpaceMetadata(clientId, pnsStats);
	}

	@Override
	public void putCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException {
		directoryService.putCredentials(list);
	}

	@Override
	public List<String[]> getCredentials(int clientId) throws DirectoryServiceException {
		return directoryService.getCredentials(clientId);
	}

	@Override
	public void updateCredentials(List<String[]> list) throws DirectoryServiceConnectionProblemException {
		directoryService.updateCredentials(list);
	}

}
