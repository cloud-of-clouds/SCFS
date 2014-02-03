package scfs.cache;

import scfs.directoryService.DirectoryService;

public interface MetadataCache extends DirectoryService {

	public void removeFromCache(String metadata);

}
