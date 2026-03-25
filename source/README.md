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


Overview
========
The Aiven Kafka Source Connector for Salesforce provides a mechanism to retrieve information from Salesforce and add it to Kafka.
It provides an *at least once* approach to sending data to Kafka this means that duplicate events can be sent, particularly around restarts.

Configuration
=============

Authentication
To make the connector work, a user has to specify Salesforce client credentials to allow it to connect to Salesforce:
1. Client Credentials are the only supported credentials at the moment
   2. [Configure Client Credentials flow documentation](https://help.salesforce.com/s/articleView?id=xcloud.configure_client_credentials_flow_for_external_client_apps.htm&type=5)
   3. specify the client credentials using the configuration options
      3. `salesforce.client.secret` for the Salesforce client secret
      4. `salesforce.client.id` for the Salesforce client id

* Define your query `salesforce.bulk.api.queries`
  * Queries are defined using the SOQL language 
  * The queries all need to have the system field LastModifiedDate included in the SELECT statement
  * The WHERE Clause is optional but should not contain the "LastModifiedDate" as this is used internally
  * Multiple queries are supported
  * example query
    * SELECT Id, Name, LastModifiedDate FROM Account;
 ``

* Some important configuration options to handle how often the Bulk API is queried by the connector
    * `salesforce.lastModifiedStartDate`
        * Let users choose when to start reading data from a specific point in the query objects
        * This is applied to all queries
    * `salesforce.soql.query.wait`
        * How long to wait between querying a Salesforce object
            * At the moment hardcoded to 60 seconds in BulkApiQueryEngine
    * `salesforce.status.check.wait`
        * Should probably rename this but how long do you wait between checking if a bulk query is ready for consumption
            * default is 5 seconds hardcoded in BulkApiQueryEngine

## Configuration

Apache Kafka has produced a users guide that describes [how to run Kafka Connect](https://kafka.apache.org/documentation/#connect_running).  This documentation describes the Connect workers configuration.  They have also produced a good description of [how Kafka Connect resumes after a failure](https://kafka.apache.org/documentation/#connect_resuming) or stoppage.


Below is an example connector configuration with descriptions:

```properties
### Standard connector configuration

## Fill in your values in these:

## These must have exactly these values:

# The Java class for the connector
connector.class=io.aiven.kafka.connect.salesforce.source.SalesforceSourceConnector

# Number of worker tasks to run concurrently
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
* We include a hash of the query, LastExecutionDate and the API name as part of the offset Key
* We include the LastModifiedDate, jobId, next Locator, recordCount in the offset data

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
