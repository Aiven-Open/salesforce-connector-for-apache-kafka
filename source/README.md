<!--
    Copyright 2026 Aiven Oy and project contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

    SPDX-License-Identifier: Apache-2
-->
Salesforce Connector for Apache Kafka
======================
This Connector has been developed by Aiven to utilise the Salesforce Bulk Api 2.0 to retrieve data from Salesforce and put the data onto various Kafka topics. 

Overview
========
The Aiven Kafka Source Connector for Salesforce provides a mechanism to retrieve information from Salesforce and add it to Kafka.
It provides an *at least once* approach to sending data to Kafka this means that duplicate events can be sent, particularly around restarts.

Configuration
=============

Authentication
To make the connector work, a user has to specify Salesforce client credentials to allow it to connect to Salesforce:
1. Client Credentials are the only supported credentials at the moment
   1. [Configure Client Credentials flow documentation](https://help.salesforce.com/s/articleView?id=xcloud.configure_client_credentials_flow_for_external_client_apps.htm&type=5)
   1. specify the client credentials using the configuration options
      1. `salesforce.client.secret` for the Salesforce client secret
      1. `salesforce.client.id` for the Salesforce client id
1. Configure your login urls
   1. `salesforce.oauth.uri` is used to authenticate against often this will be  "https://MyCompany.my.salesforce.com/services/oauth2/token"
      1. More details can be found on the Salesforce [website](https://help.salesforce.com/s/articleView?id=xcloud.remoteaccess_oauth_endpoints.htm&type=5)
   1. `salesforce.uri` is the api we query against and is often "https://MyCompany.my.salesforce.com"
1.  The api version you wish to use of the Bulk API is also selectable
   1.`salesforce.api.version` can be set to select a particular version e.g. 'v66.0', 'v60.0' etc
   2. By default, version `v65.0` is selected


* Define your query `salesforce.bulk.api.queries`
  * Queries are defined using the SOQL language and are subject to SOQL Syntax and case sensitivity restrictions
  * The queries all need to have the system field LastModifiedDate included in the SELECT statement
  * The WHERE Clause is optional but should not contain the "LastModifiedDate" as this is used internally
  * The Order By statement is not allowed to be used as this is used to order the query results 
  * Multiple queries are supported
  * example query
    * SELECT Id, Name, LastModifiedDate FROM Account;
  * It is possible to get duplicate entries in particular if your SELECT statement does not include a field and that field is updated the LastModifiedDate will be updated and you will receive a new event from that entry.
  * The Connector does not support using sub queries as part of your bulk api query
 ``

* Some important configuration options to handle how often the Bulk API is queried by the connector
    * `salesforce.soql.query.wait`
      * How long to wait between querying a Salesforce object
        * The default is 60 seconds, but if this data does not change very often we would recommend extending this delay to help preserve your API limits
    * `salesforce.status.check.wait`
      * How long after submitting a query  to Salesforce do you wait between checking if a bulk query is ready for consumption
        * default is 5 seconds, if you are expecting a large amount of data you may need to extend this time to reduce the number of api calls. 
    * 'salesforce.max.records'
      * Defines how many records per page should be returned by the BulkApi, with larger Objects this may need to be decreased to allow for smaller chunking of data to the api
        * The default is 50,000 records
* This connector supports just one task this helps prevent issues with timing and also prevents using up the api calls available to your account too quickly.
  * See further details on Salesforce allocations and limits on the [Bulk API 2.0](https://developer.salesforce.com/docs/atlas.en-us.api_asynch.meta/api_asynch/bulk_common_limits.htm)



## Configuration

Apache Kafka has produced a users guide that describes [how to run Kafka Connect](https://kafka.apache.org/documentation/#connect_running).  This documentation describes the Connect workers configuration.  They have also produced a good description of [how Kafka Connect resumes after a failure](https://kafka.apache.org/documentation/#connect_resuming) or stoppage.


Below is an example connector configuration with descriptions:

```properties
### Standard connector configuration

## Fill in your values in these:

# The Java class for the connector
connector.class=io.aiven.kafka.connect.salesforce.source.SalesforceSourceConnector

# Number of worker tasks to run concurrently, only '1' is supported on this connector
tasks.max=1

# All data will be produced to topics with the prefix using the below an example topic would be
# salesforce.test.bulkapi.Account
topics.prefix=salesforce.test

# The maximum number of times to retry a query or authentication request against the Bulk API before failing
max.retries=3

# The version of the Salesforce API to use for all queries
salesforce.api.version=v65.0

# Salesforce Client Secret
salesforce.client.secret=YOUR_CLIENT_SECRET

# Salesforce Client Id
salesforce.client.id=YOUR_CLIENT_ID

# The Salesforce OAUTH Uri may be unique for your salesforce deployment
salesforce.oauth.uri=https://www.salesforce.com/services/oauth2/token

salesforce.bulk.api.queries=Select Id,Name,LastModifiedDate FROM Account;Select Id,LastModifiedDate,Department,Email,FirstName,LastName FROM Contact WHERE HasOptedOutOfEmail=False;

# Optional
# Wait at least an hour in between each query being called
# Time is in seconds
salesforce.soql.query.wait=3600

# When you have large Object tables it can take longer for the job to be processed by the API
# reduce the number of API calls by increasing the time between job status checks
# Time is in seconds
salesforce.status.check.wait=120


# Begin the Query from a specific place
salesforce.lastModifiedStartDate=2025-11-08T00:00:00Z
```

Offsets
=======
How the offsets are handled
* We include a hash of the query,the API name as part of the offset Key
* We include the LastModifiedDate, jobId, next Locator, recordCount in the offset data
* As we order the result set that is returned we then are able to use the lastModifiedDate in the offset, on a restart we read the offset and seed the correct lastModifiedDate


Features
============
An Apache Kafka source connector for Salesforce to consume data from Salesforce and to publish that data to Kafka topics

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
