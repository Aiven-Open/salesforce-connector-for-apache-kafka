/*
 * Copyright 2026 Aiven Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aiven.kafka.connect.salesforce.sink;

import io.aiven.kafka.connect.salesforce.sink.config.SalesforceSinkConfig;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/** A task that writes records to Salesforce. */
public final class SalesforceSinkTask extends SinkTask {
	private static final Logger LOG = LoggerFactory.getLogger(SalesforceSinkTask.class);

	private SalesforceSinkConfig config;

	/** Constructor */
	public SalesforceSinkTask() {
	}

	@Override
	public String version() {
		return "TODO: Bring in the strings version";
	}

	@Override
	public void start(final Map<String, String> props) {
		Objects.requireNonNull(props, "props cannot be null");
		LOG.info("Start Salesforce sink task ({})", config.getSinkObject());
		this.config = new SalesforceSinkConfig(props);
	}

	@Override
	public void put(Collection<SinkRecord> records) {
	}

	@Override
	public void stop() {
		LOG.info("Stop Salesforce sink task");
	}
}
