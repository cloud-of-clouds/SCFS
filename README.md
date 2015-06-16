### Introduction

SCFS is a cloud-backed file system that provides strong consistency even on top of eventually-consistent cloud storage services.
It is build on top of FUSE, thus providing a POSIX-like interface. SCFS provides also a pluggable backend that allows it to work with a single cloud or with a cloud-of-clouds.
The design, implementation and evaluation of the system is described in our USENIX ATC'14 paper.

Its main concern is address some of the problems presented on the other existing cloud-backed file systems:

Most of them not support controlled file sharing among clients.
Some, use a proxy to connect with the cloud storage, which is a single point of failure, once if it is down, no client can access to the data stored at the cloud side.
Almost all file systems use only a single cloud as backend.
Architecture

The figure bellow presents the architecture of SCFS with its three main components: the backend cloud storage for maintaining the file data; the coordination service for managing the metadata and to support synchronization; and the SCFS Agent that implements most of the SCFS functionality, and corresponds to the file system client mounted at the user machine.

<FIGURE>

The figure shows the backend cloud storage as cloud-of-clouds formed by Amazon S3, Google Storage, RackSpace Cloud Files and Windows Azure.
To store data in this clouds, SCFS uses DepSky. More specifically, uses DepSky-CA protocol once we want address the main cloud storage limitations (see DepSky).
SCFS allows also a different backend formed by only one cloud.

The coordination services used by SCFS are DepSpace and ZooKepper. In the figure above they are installed in computing clouds, but they can be installed on any IaaS or in any server. To get more information about theses system please read the referred papers. DepSpace is described on a EuroSys'08 paper and ZooKepper on a Usenix'10 paper.

The SCFS Agent component is the one that implements the most features of the file system. It makes a lot of usage of cache techniques to improve the system performance, to reduce the monetary costs and to increase the data availability. More Specifically it uses a temporary small memory cache to absorb some metadata update bursts, a main memory cache to maintain data of open files, and a bigger disk cache to save all the most recents files used by the client. The last one uses all the free space in disk. After the disk is full it uses LRU policies to create new space.

##### Operation Modes

SCFS is a very configurable file system, once clients can use it according with their needs.
There is four main configurations that change both the file system behaviour as the provided guarantees:
The system can be configured to use only one cloud or a cloud-of-clouds for both the backend cloud storage as to the coordination service.
The client can choose if it uses the main memory cache to store open files data, or not. The sending of data to the clouds can be synchronous or asynchronous.
The system can be sharing or non-sharing. If non-sharing configuration is used the system does not uses none coordination service.
Is considered that all files are private (not shared), therefore all the metadata can be stored in the storage clouds.


If you have any question, take a look at the [site](http://cloud-of-clouds.github.io/SCFS/), [wiki](https://github.com/cloud-of-clouds/SCFS/wiki/Getting-Started-with-SCFS), or to contact us!

***

### Table of contents


- [Getting Started with SCFS](#getting-started-with-scfs)
	- [Setting up DepSky](#setting-up-depsky)
	- [Setting up DepSpace](#setting-up-depspace)
	- [Deploying DepSpace](#deploying-depspace)
	- [Mounting SCFS](#mounting-scfs)
		- [Non-Sharing SCFS](#non-sharing-scfs)
	- [Using SCFS](#using-scfs)
***

### Getting Started with SCFS 
Here you can find how to configure, install and use the available version of SCFS. The version we provide uses DepSpace as coordination service and Amazon S3 as single cloud storage backend. 

The first step is downloadd the latest version available of SCFS. After that you need to extract the archive downloaded. Make sure you have java 1.7 or later installed.

Next, you need to link the system with the Fuse on your machine. To do that, you just need to run a script we provide in the project's root directory. If you have a x64 Java installed, you just need to run:

`./configure_x64.sh`

In the other hand, if your java is a x86 version, please run:

`./configure_x86.sh`

In the next step you just need to set your JAVA_HOME and FUSE_HOME in the build.conf file. Usually, your fuse home will be in /usr/local. The JAVA_HOME will depend on the java version you have installed on your machine.

The next step is to configure the remain configuration files. More specifically, it is necessary to fill configuration files for DepSky and DepSpace. The following subsections explain how to do it.

### Setting up DepSky
To fill DepSky configuration files please read the _'Getting Started with DepSky'_ section in DepSky page. Here you can learn how to create the cloud storage accounts and where you can find the necessary API keys.

If you want to share files with other users, you have also to fill the canonicalId field in each entry of the accounts.properties. The version avilable of the system only allows sharing files between different accounts if you use only Amazon S3 (both in one single cloud and cloud-of-clouds backends). You can find your canonical Id in the Security Credentials page of your AWS Management Console.

You can use also SCFS with only one cloud storage as backend. For now, we only support Amazon S3. To configure it you just need to delete the last three entries in the account.properties file (see HowToUseDepSky). Your file must be equal to the content below:

```
#AMAZON
driver.type=AMAZON-S3
driver.id=cloud1
accessKey=********************
secretKey=****************************************
location=EU_Ireland
canonicalId=******************************************************
```

### Setting up DepSpace

[DepSpace](https://github.com/bft-smart/depspace) configuration consists in two main steps; the address and port configuration of all DepSpace's replicas, and the configuration of system parameters. This could be done in config/hosts.config and config/system.config files respectively. To comment out a configuration parameter in those files, the line must start with the character #.

DepSpace is built on top of [BFT-SMaRt](https://github.com/bft-smart/library). Please take a look at _How to run BFT-SMaRt_.

In _hosts.config_ file you can set the address and port of all DepSpace replicas. The configuration file should look like this:

```
#server id, address and port (the ids from 0 to n-1 are the service replicas) 
0 127.0.0.1 11000
1 127.0.0.1 11010
2 127.0.0.1 11020
3 127.0.0.1 11030
4 127.0.0.1 11040
5 127.0.0.1 11050
6 127.0.0.1 11060
7 127.0.0.1 11070
```

After the configuration of the replicas address and port, you can set the parameter Besides the parameters you need to set to BFT-SMaRt, there are others you need to configure to DepSpace. These parameters are described below:

Parameter | Description | Values | Default value
----------|-------------|--------|--------------
system.general.rdRetryTime | The time the system blocks on rd operation waiting for a match tuple in the space. SCFS does not uses this operation. | Integer (miliseconds) |10000
system.general.realTimeRenew | Use or not of renewable tuples. Note that SCFS needs this parameter to be true. | true or false | true
system.general.logging | Use or not of logging. Set this parameter to true if you want it. | true or false | false
system.general.logging.mode | Set the logging mode. The modes are: 0=(no logging), 1="rw", 2="rws", (any other)="rwd" | Integer | 0
system.general.batching | Use batches of messages. We recommend you to set this to false. | true or false | false
system.general.checkpoint_period | The period (number of operation) in which the system performs a checkpoint of its state. | Integer | 1000
system.general.replaceTrigger | The usage or not of the DepSpace's replace trigger layer. Note that SCFS needs this layer to work correctly. | true or false | true

We recommend you to use the default values set in the _config/system.config_ file available.

#### Public Keys
We provide some default DepSpace keys. However, if you need to generate new public/private keys for DepSpace replicas or clients, you can use the following command to generate files for public and private keys:

`./rsaKeyPairGenerator.sh <id of the replica/client>`

Keys are stored in the _config/keys_ folder. The command above creates key pairs both for clients and replicas. Currently such keys need to be manually distributed.

#### Using DepSpace with just 1 replica
The default _system.config_ and _hosts.config_ files are set to use Byzantine Fault tolerant DepSpace (using 3f+1 replicas).

To use DepSpace without any fault tolerance guarantee, some modification need to be done in the _config/system.config_ file. In this way you need to set the following parameters of this file:

```
#Number of servers in the group
system.servers.num = 1
```
```
#Maximum number of faulty replicas 
system.servers.f = 0
```
```
#Initial View 
system.initial.view = 0
```

You also need to reconfigure the replicas' address/port. To do that, you should update your _config/hosts.config_ file to look like the following:

```
#server id, address and port (the ids from 0 to n-1 are the service replicas) 
0 127.0.0.1 11000
```

### Deploying DepSpace
To deploy DepSpace in a server (being it in the clouds or in other location), you first need to get the files to deploy. This files can be generated by executing the command bellow:

`./generateDepSpaceToDeploy.sh`

This script will generate a zip file (DepSpace.zip) with the configurations you did for the DepSpace locally (namely the _hosts.config_ file). To deploy DepSpace other sites, you just need to upload de generated file to these sites and unzip it:

`unzip DepSpace.zip`

After unzip the file, to run the DepSpace in each site, you just need to run the _runDepSpace.sh_ script in each site. You can find this script inside the DepSpace folder after you unzip the file. To execute this script you should run the follow command:

`./runDepSpace.sh <replicaID>`

### Mounting SCFS
To mount SCFS you just need to run the following command in the root of the project:

`./mountSCFS.sh <mountPoint> <clientID>`

The meaning of the two given arguments are:

* _mountPoint_ - the folder where SCFS will be mounted;

* _clientID_ - the identifier of the client (must be unique).

Each time you mount the system you are able to define how the system will behave, opting for several parameters. This parameters will have influence in the system guarantees and performance. Note that the default behavior of the system (the command presented before) provides the highest level of guarantees and the lower performance.

In the table bellow are shown all the flags that can be turned on:

Parameter             | Description | Default value
----------------------|-------------|--------------
-optimizedCache | Use of main memory data cache. | off
--sync-to-cloud	| The FSYNC POSIX operation flushes the data to the clouds. If turned off, FSYNC flushes data to local disk. | off
--non-blocking-cloud | Data is sent to the clouds asynchronously. If turned off, the file close operation will block when data are sent to the clouds. | off
--non-sharing | The system runs in non-sharing mode of operation. It means that clients are not able to share data between them | off
-printer | Turn on the debug mode. | off

As you can understand, if the --non-blocking-cloud flag is turned on, the performance of the system is increased while the guarantees offered by the close operation decrease.

#### Non-Sharing SCFS
To use this operation mode, you should turn on the _--non-sharing_ flag as shown bellow:

`./mountSCFS.sh <mountPoint> <clientID> --non-sharing`

This operation mode is ideal to use the system with its highest performance, backuping the file system data to the clouds. So, the use of _--non-blocking-cloud_ flag are recommended:

`./mountSCFS.sh <mountPoint> <clientID> --non-sharing --non-blocking-cloud`

Note: Since all files are personal in this operation mode, it does not need the DepSpace deployment. So, if you want to try SCFS without experience the deployment effort of DepSpace, this is the perfect option to do that.

## Using SCFS
To use SCFS you just need to enter in the _<mountPoint>_ folder and operate in the same way you do in your local filesystem. For example, let your SCFS configuration be:

`./mountSCFS.sh /scfs 4 --non-sharing --non-blocking-cloud -optimizedCache`

In the example, your _<mountPoint>_ will be _/scfs_. So, you just need to operate over the files in that folder to take advantage of SCFS.

#### Share a file using SCFS
To share a file, you just need to use the setfacl command, giving the clientId of the client with which you want to share a file, and the permissions to that client:

`setfacl -m u:<friendClientID>:<permissions> <fileName>`

For example, assuming there is a file named _file1.txt_, to share this file with the client 5 giving full control over the file, you just need to run the following command:

`setfacl -m u:5:rwx file1.txt`