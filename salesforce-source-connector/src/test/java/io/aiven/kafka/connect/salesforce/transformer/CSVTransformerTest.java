package io.aiven.kafka.connect.salesforce.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aiven.kafka.connect.salesforce.common.Transformer.CsvTransformer;
import io.aiven.kafka.connect.salesforce.common.config.SalesforceConfigFragment;
import io.aiven.kafka.connect.salesforce.config.SalesforceSourceConfig;
import io.aiven.kafka.connect.salesforce.model.BulkApiSourceData;
import io.aiven.kafka.connect.salesforce.model.BulkApiSourceRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.kafka.connect.json.JsonConverter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import static org.mockito.Mockito.mock;

public class CSVTransformerTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(CSVTransformerTest.class);
  private SalesforceConfigFragment configFragment;
  private SalesforceSourceConfig commonConfig;

  private JsonConverter jsonConverter;

  private CsvTransformer transformer;

  private ObjectMapper mapper = new ObjectMapper();


  @Test
  void stepThroughCSVTest() throws IOException {
    jsonConverter = new JsonConverter();
    Reader in = new FileReader("src/test/resources/values.csv");

    List<CSVRecord> records = CSVFormat.RFC4180.builder().setHeader().get().parse(in).getRecords();
    configFragment = mock(SalesforceConfigFragment.class);
    commonConfig = mock(SalesforceSourceConfig.class);
    BulkApiSourceData data = new BulkApiSourceData(records,"values","12-02-2026",configFragment);
    BulkApiSourceRecord sourceRecord = new BulkApiSourceRecord(records, "values" );
    sourceRecord.setContext(data.extractContext(sourceRecord.getNativeItem()).orElseGet(null));
    transformer = new CsvTransformer(commonConfig, jsonConverter);

    transformer.generateRecords(data,sourceRecord).forEach(entry -> {
      try {
        LOGGER.info("Entry from csvTransformer {}", mapper.writeValueAsString(entry));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    });

  }
}
