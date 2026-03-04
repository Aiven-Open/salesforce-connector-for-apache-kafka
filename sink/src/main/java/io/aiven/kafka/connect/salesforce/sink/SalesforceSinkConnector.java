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

import io.aiven.kafka.connect.salesforce.sink.config.SalesforceSinkConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** The connector that coordinates tasks to write records to Salesforce. */
public final class SalesforceSinkConnector extends SinkConnector {

	private static final Logger LOG = LoggerFactory.getLogger(SalesforceSinkConnector.class);

	private static final String TASK_ID = "task.id";

	private Map<String, String> configProperties;

	/** Constructor */
	public SalesforceSinkConnector() {
	}

	@Override
	public String version() {
		return "TODO: 1.0.0 commons-strings Version.version()";
	}

	@Override
	public SalesforceSinkConfigDef config() {
		return new SalesforceSinkConfigDef();
	}

	@Override
	public Class<? extends Task> taskClass() {
		return SalesforceSinkTask.class;
	}

	@Override
	public List<Map<String, String>> taskConfigs(int maxTasks) {
		final var taskProps = new ArrayList<Map<String, String>>();
		for (int i = 0; i < maxTasks; i++) {
			final var props = new HashMap<>(configProperties);
			props.put(TASK_ID, String.valueOf(i));
			taskProps.add(props);
		}
		return taskProps;
	}

	@Override
	public void start(Map<String, String> props) {
		Objects.requireNonNull(props, "properties haven't been set");
		LOG.info("Start Salesforce sink connector");
		this.configProperties = props;
	}

	@Override
	public void stop() {
		LOG.info("Stop Salesforce sink connector");
	}
}
