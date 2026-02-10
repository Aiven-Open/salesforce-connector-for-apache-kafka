/*
 * Copyright 2026 Aiven Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aiven.kafka.connect.salesforce.common.Transformer;

import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import io.aiven.commons.kafka.connector.source.task.Context;
import io.aiven.commons.kafka.connector.source.transformer.InputStreamTransformer;
import org.apache.commons.io.function.IOSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.json.JsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This is a csv transformer taking CSV and changing it to a JSON payload for
 * Kafka
 */
public class CsvTransformer extends InputStreamTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(CsvTransformer.class);
	private final JsonConverter jsonConverter;

	/**
	 * Initialize the CsvTransformer and configure the converter and add the
	 * configuration for the transformer
	 * 
	 * @param config
	 *            The source common config used to configure the transformer
	 * @param jsonConverter
	 *            The converter used to convert the byte data into the right format
	 *            for Kafka
	 */
	public CsvTransformer(SourceCommonConfig config, JsonConverter jsonConverter) {
		super(config);
		this.jsonConverter = jsonConverter;
		jsonConverter.configure(Map.of("schemas.enable", "false"), false);
	}

	/**
	 * The createSpliterator will take the input stream and then parse through it,
	 * adding SchemaAndValue Consumer Object for each of the identified csv entries,
	 * each entry should be separated by
	 * System.lineSeparator().getBytes(Charset.defaultCharset());
	 *
	 * @param ioSupplier
	 *            The input stream wrapped in a supplier
	 * @param streamLength
	 *            The total length of the stream can be null
	 * @param context
	 *            The context of the data record
	 * @return A StreamSpliterator which chunks the stream into individual records
	 */
	@Override
	protected StreamSpliterator createSpliterator(IOSupplier<InputStream> ioSupplier, long streamLength,
			Context<?> context) {
		return new StreamSpliterator(LOGGER, ioSupplier) {
			BufferedReader reader;

			@Override
			protected void inputOpened(final InputStream input) {
				reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

			}

			@Override
			public void doClose() {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						LOGGER.error("Error closing reader: {}", e.getMessage(), e);
					}
				}
			}

			@Override
			public boolean doAdvance(final Consumer<? super SchemaAndValue> action) {
				String line = null;
				try {
					// remove blank and empty lines.
					while (StringUtils.isBlank(line)) {
						line = reader.readLine();
						if (line == null) {
							// end of file
							return false;
						}
					}
					line = line.trim();
					// toConnectData does not actually use topic in the conversion so its fine if it
					// is null.
					action.accept(jsonConverter.toConnectData(context.getTopic().orElse(null),
							line.getBytes(StandardCharsets.UTF_8)));
					return true;
				} catch (IOException e) {
					LOGGER.error("Error reading input stream: {}", e.getMessage(), e);
					return false;
				}
			}
		};
	}

}
