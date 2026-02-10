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
package io.aiven.kafka.connect.salesforce.transformers;

import io.aiven.commons.kafka.connector.source.AbstractSourceRecord;
import io.aiven.commons.kafka.connector.source.NativeSourceData;
import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import io.aiven.commons.kafka.connector.source.task.Context;
import io.aiven.commons.kafka.connector.source.transformer.InputStreamTransformer;
import io.aiven.commons.kafka.connector.source.transformer.Transformer;
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
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A test to run this particular source connector with csv data onto topics
 */
public class JsonTransformer extends Transformer {
	private final JsonConverter jsonConverter;

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonTransformer.class);

	/**
	 * Set the JsonConverter for this transformer
	 * 
	 * @param jsonConverter
	 *            A configured JsonConverter
	 *
	 * @param sourceCommonConfig
	 *            A source Common Config
	 */
	public JsonTransformer(final JsonConverter jsonConverter, SourceCommonConfig sourceCommonConfig) {
		super(sourceCommonConfig);
		this.jsonConverter = jsonConverter;
	}

	/**
	 * This is here as I transition to the new Transformers
	 * 
	 * @param inputStreamIOSupplier
	 *            boop
	 * @param streamLength
	 *            boop
	 * @param context
	 *            boop
	 * @param sourceConfig
	 *            boop
	 * @return boop
	 */
	public InputStreamTransformer.StreamSpliterator createSpliterator(
			final IOSupplier<InputStream> inputStreamIOSupplier, final long streamLength, final Context<?> context,
			final SourceCommonConfig sourceConfig) {
		return new InputStreamTransformer.StreamSpliterator(LOGGER, inputStreamIOSupplier) {
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

	/**
	 * 
	 * @param nativeSourceData
	 *            boop
	 * @param t
	 *            di boop
	 * @return dada
	 * @param <T>
	 *            as boop
	 */
	@Override
	public <T extends AbstractSourceRecord<?, ?, ?, T>> Stream<SchemaAndValue> generateRecords(
			NativeSourceData<?, ?, ?, T> nativeSourceData, T t) {
		return Stream.empty();
	}

	/**
	 * 
	 * @param abstractSourceRecord
	 *            la di da
	 * @return Schema and Value of the Schema da li la
	 */
	@Override
	public SchemaAndValue generateKeyData(AbstractSourceRecord<?, ?, ?, ?> abstractSourceRecord) {
		return new SchemaAndValue(abstractSourceRecord.getValue().schema(),
				((String) abstractSourceRecord.getValue().value()).getBytes(StandardCharsets.UTF_8));
	}
}
