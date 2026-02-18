Salesforce Connector for Apache Kafka
======================


Overview
========


TODO
========
Query Manipulation
* The queries all need to have the system fields Id and LastModifiedDate
* So we need to add them if they are missing to the query so we can use them for the offsets

Offsets
How the offsets should be built
* The offsets require some info from the query 
* We should include the Id and LastModifiedDate in the offset data
* We should include the query string as a hash in the key as well as the api identifier so that we can use different apis in the one connector
  * This way if a user executes Select Id,Name,a From Account and Select Id,Name,b From Account we will see these both as different entries and pull back all the data.
* We need to add a few configuration options
  * lastModifiedStartDate
    * Let users choose when to start the lastModifiedStartDate from
  * DELTA_BETWEEN_QUERIES 
    * How long to wait between querying a Salesforce object
      * At the moment hardcoded to 60 seconds in BulkApiQueryEngine
  * WAIT_BETWEEN_QUERIES
    * Should probably rename this but how long do you wait between checking if a bulk query is ready for consumption
      * default is 5 seconds hardcoded in BulkApiQueryEngine


Delta LastModifiedDate
* The LastModifiedDate is currently taken from when we start the process of executing the query, the query actually returns a creation date that we should use instead as this will help prevent duplicate events
* The LastModifiedDate we should try and get the last time the query was run from the offsets so we can return to the previous time immediately and not start processing again on start up

Features
============
A source connector for salesforce to consume data from Salesforce and to publish that data to Kafka topics
Setup
============

### Prerequisites
* Java 17
* Maven

License
============
Salesforce Connector for Apache Kafka is licensed under the Apache License, version 2.0. Full license text is available in the [LICENSE](LICENSE) file.

Please note that the project explicitly does not require a CLA (Contributor License Agreement) from its contributors.

Contact
============
Bug reports and patches are very welcome, please post them as GitHub issues and pull requests at https://github.com/aiven/{{PROJECT_NAME}} .
To report any possible vulnerabilities or other serious issues please see our [security](SECURITY.md) policy.
