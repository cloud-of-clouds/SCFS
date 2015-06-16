SCFS is a cloud-backed file system that provides strong consistency even on top of eventually-consistent cloud storage services. It is build on top of FUSE, thus providing a POSIX-like interface. SCFS provides also a pluggable backend that allows it to work with a single cloud or with a cloud-of-clouds. The design, implementation and evaluation of the system is described in our USENIX ATC'14 paper.

Its main concern is address some of the problems presented on the other existing cloud-backed file systems:

Most of them not support controlled file sharing among clients.
Some, use a proxy to connect with the cloud storage, which is a single point of failure, once if it is down, no client can access to the data stored at the cloud side.
Almost all file systems use only a single cloud as backend.
Architecture

The figure bellow presents the architecture of SCFS with its three main components: the backend cloud storage for maintaining the file data; the coordination service for managing the metadata and to support synchronization; and the SCFS Agent that implements most of the SCFS functionality, and corresponds to the file system client mounted at the user machine.

<FIGURE>

The figure shows the backend cloud storage as cloud-of-clouds formed by Amazon S3, Google Storage, RackSpace Cloud Files and Windows Azure. To store data in this clouds, SCFS uses DepSky. More specifically, uses DepSky-CA protocol once we want address the main cloud storage limitations (see DepSky). SCFS allows also a different backend formed by only one cloud.

The coordination services used by SCFS are DepSpace and ZooKepper. In the figure above they are installed in computing clouds, but they can be installed on any IaaS or in any server. To get more information about theses system please read the referred papers. DepSpace is described on a EuroSys'08 paper and ZooKepper on a Usenix'10 paper.

The SCFS Agent component is the one that implements the most features of the file system. It makes a lot of usage of cache techniques to improve the system performance, to reduce the monetary costs and to increase the data availability. More Specifically it uses a temporary small memory cache to absorb some metadata update bursts, a main memory cache to maintain data of open files, and a bigger disk cache to save all the most recents files used by the client. The last one uses all the free space in disk. After the disk is full it uses LRU policies to create new space.

Operation Modes

SCFS is a very configurable file system, once clients can use it according with their needs. There is four main configurations that change both the file system behaviour as the provided guarantees:

The system can be configured to use only one cloud or a cloud-of-clouds for both the backend cloud storage as to the coordination service. The client can choose if it uses the main memory cache to store open files data, or not. The sending of data to the clouds can be synchronous or asynchronous. The system can be sharing or non-sharing. If non-sharing configuration is used the system does not uses none coordination service. Is considered that all files are private (not shared), therefore all the metadata can be stored in the storage clouds.

If you have any question, take a look at the [site](http://cloud-of-clouds.github.io/SCFS/, [wiki](https://github.com/cloud-of-clouds/SCFS/wiki/Getting-Started-with-SCFS), or to contact us!
