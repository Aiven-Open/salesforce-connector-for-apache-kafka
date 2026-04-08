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
Salesforce Source Connector for Apache Kafka
======================
This Connector has been developed by Aiven to utilise the Salesforce Bulk API 2.0 to retrieve data from Salesforce and put the data onto various Kafka topics. 

Overview
========
The Aiven Kafka Source Connector for Salesforce provides a mechanism to retrieve information from Salesforce and add it to Kafka.
It provides an *at least once* approach to sending data to Kafka. This means that duplicate events can be sent, particularly around restarts.

Configuration
=============

Full [configuration documentation](https://aiven-open.github.io/salesforce-connector-forapache-kafka/source/configuration.html) can be found on our [documentation site]((https://aiven-open.github.io/salesforce-connector-forapache-kafka/source).

An [example configuration file](https://aiven-open.github.io/salesforce-connector-forapache-kafka/source/config_example.txt) is also available for download.

Offsets
=======
How the offsets are handled
* We include a hash of the query, the API name as part of the offset Key
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

### Releases
Visit our release page for [release information](https://github.com/Aiven-Open/salesforce-connector-for-apache-kafka/releases).

License
============
Salesforce Connector for Apache Kafka is licensed under the Apache License, version 2.0. Full license text is available in the [LICENSE](LICENSE) file.

Please note that the project explicitly does not require a CLA (Contributor License Agreement) from its contributors.

Contact
============
Bug reports and patches are very welcome, please post them as GitHub issues and pull requests at https://github.com/aiven/{{PROJECT_NAME}} .
To report any possible vulnerabilities or other serious issues please see our [security](SECURITY.md) policy.
