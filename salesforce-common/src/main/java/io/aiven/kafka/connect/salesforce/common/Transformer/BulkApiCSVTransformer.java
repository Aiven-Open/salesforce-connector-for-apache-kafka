package io.aiven.kafka.connect.salesforce.common.Transformer;

import io.aiven.commons.kafka.connector.source.config.SourceCommonConfig;
import io.aiven.commons.kafka.connector.source.task.Context;
import io.aiven.commons.kafka.connector.source.transformer.Transformer;
import org.apache.commons.io.function.IOSupplier;
import org.apache.kafka.connect.data.SchemaAndValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class BulkApiCSVTransformer extends  Transformer {
  /**
   * Creates the stream spliterator for this transformer.
   *
   * @param inputStreamIOSupplier
   *            the input stream supplier.
   * @param streamLength
   *            the length of the input stream, {@link #UNKNOWN_STREAM_LENGTH} may
   *            be used to specify a stream with an unknown length, streams of
   *            length zero will log an error and return an empty stream
   * @param context
   *            the context
   * @param sourceConfig
   *            the source configuration.
   * @return a StreamSpliterator instance.
   */
  @Override
  protected StreamSpliterator createSpliterator(IOSupplier<InputStream> inputStreamIOSupplier, long streamLength, Context<?> context,
                                                SourceCommonConfig sourceConfig) {
    return new StreamSpliterator() {

      @Override
      protected boolean doAdvance(Consumer<? super SchemaAndValue> consumer) {
        return false;
      }

      @Override
      protected void doClose() {

      }

      @Override
      protected void inputOpened(InputStream inputStream) throws IOException {

      }
    };
  }

  /**
   * Convert the native key into a Schema and Value for Kafka.
   *
   * @param nativeKey
   *            the native key to convert.
   * @param topic
   *            the topic for the conversion.
   * @param sourceConfig
   *            the SourceCommonConfig for the conversion.
   * @return a SchemaAndValue for the key.
   */
  @Override
  public SchemaAndValue getKeyData(Object nativeKey, String topic, SourceCommonConfig sourceConfig) {
    return null;
  }
}
