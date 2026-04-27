<!--
Copyright 2026 Aiven Oy

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

SPDX-License-Identifier: Apache-2.0
-->

Salesforce Sink Connector for Apache Kafka
==============================================================================

Overview
------------------------------------------------------------------------------

The Aiven Kafka Sink Connector for Salesforce takes messages published to Kafka topics and inserts them to Salesforce objects.
It provides an *at least once* delivery guarantee. This means that duplicate records can be sent to Salesforce, particularly around restarts or failures.

Configuration
------------------------------------------------------------------------------

Full [configuration documentation](https://aiven-open.github.io/salesforce-connector-for-apache-kafka/sink/configuration.html) can be found on our [documentation site](https://aiven-open.github.io/salesforce-connector-for-apache-kafka/sink).

An [example configuration file](https://aiven-open.github.io/salesforce-connector-for-apache-kafka/sink/config_example.txt) is also available for download.

How It Works
------------------------------------------------------------------------------

The sink connector consumes records from Kafka topics and writes them to Salesforce objects using the Bulk API 2.0.

### Data Format

The connector expects records with **Struct** schemas in the value field. The struct field names are dynamically mapped to Salesforce object field names:

```
Kafka Record Value (Struct):
{
  "Name": "Alice",
  "Email": "alice@example.com",
  "ExternalId": "EXT001"
}
```

This will be written to Salesforce with columns: `Name`, `Email`, `ExternalId`.

### Processing Flow

1. **Buffering**: Records are buffered in memory until `flush()` is called by the Kafka Connect framework
2. **Schema Detection**: The connector analyzes all buffered records to discover the complete set of field names
3. **CSV Generation**: Records are converted to CSV format with a header row containing all discovered field names
4. **Bulk Insert**: Data is submitted to Salesforce as a multipart insert job using Bulk API 2.0
5. **Job Polling**: The connector waits for the Salesforce job to complete before committing offsets
6. **Offset Commit**: Only after successful completion are the Kafka offsets committed

### Flush Interval

The frequency of flushes (and thus Salesforce inserts) is controlled by the `offset.flush.interval.ms` configuration in Kafka Connect. The default is typically 60 seconds.

Releases
==============================================================================

Visit our release page for [release information](https://github.com/Aiven-Open/salesforce-connector-for-apache-kafka/releases).

Current Limitations
==============================================================================

- **Insert only**: Currently only supports `insert` operations. Update and upsert operations are not yet implemented
- **Dynamic schema**: Field names are discovered dynamically from records. This provides flexibility but may lead to schema inconsistencies
- **Batch processing**: All records in a flush are sent as a single batch. Large batches may hit Salesforce API limits

License
==============================================================================

Salesforce Connector for Apache Kafka is licensed under the Apache License, version 2.0. Full license text is available in the [LICENSE](LICENSE) file.

Please note that the project explicitly does not require a CLA (Contributor License Agreement) from its contributors.

Contact
==============================================================================

Bug reports and patches are very welcome, please post them as GitHub issues and pull requests at https://github.com/aiven/salesforce-connector-for-apache-kafka .
To report any possible vulnerabilities or other serious issues please see our [security](SECURITY.md) policy.
