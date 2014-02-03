
About SCFS:

- Creating the cloud accounts
First you need to create the accounts at Amazon S3 (http://aws.amazon.com/pt/s3/), RaskSpace (http://www.rackspace.co.uk/), Windows Azure (https://www.windowsazure.com/en-us/) and Google Storage (https://developers.google.com/storage/). After create all necessary accounts, you need to fill the access credentials in the 'accounts.properties' file at config folder.
If you only want to use Amazon S3 as your cloud storage provider, you can only create an account at Amazon S3 and use the example file (config/accounts_amazon.properties). To do that, copy the content of the 'accounts_amazon.properties' file to the one mencioned before (aconfig/accounts.properties). In this case will be used four diferent Amazon S3 locations to store the data (US_Standard, EU_Ireland, US_West and AP_Tokyo).


- Using
To use SCFS just run the script 'runSCFS.sh' at the root of the project. Make sure you have java 7 installed before running SCFS. After running the script mencioned above, the system will be mounted in the SCFS folder at the root of the project. All operations inside that folder (mkdir, write to a file, etc) will have a SCFS response.  


- More Information
This SCFS version dont have any garbage collector implemented (notice that the delete operation dont erase the file of the cloud due to a performance and protocol issue). So you can only erase your stored data manually.
If you want some program to do that let us know.

	
