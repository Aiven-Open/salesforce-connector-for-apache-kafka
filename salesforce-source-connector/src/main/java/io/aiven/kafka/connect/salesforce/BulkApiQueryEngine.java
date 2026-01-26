package io.aiven.kafka.connect.salesforce;

import io.aiven.kafka.connect.salesforce.common.config.SalesforceConfigFragment;
import io.aiven.kafka.connect.salesforce.model.AbortJob;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.aiven.kafka.connect.salesforce.model.JobState;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static io.aiven.kafka.connect.salesforce.model.JobState.Failed;

/**
 * The BulkApiQueryEngine handles taking the config from the connector and
 * making the relevant queries to the Salesforce BulkApi 2.0 It handles the
 * lifecycle of the requests along ith exceptions
 */
public class BulkApiQueryEngine {
	private static Logger LOGGER = LoggerFactory.getLogger(BulkApiQueryEngine.class);
	private SalesforceConfigFragment configFragment;
	private BulkApiClient apiClient;
	private LinkedList<String> queries;

	public BulkApiQueryEngine(SalesforceConfigFragment configFragment, BulkApiClient apiClient,
			LinkedList<String> queries) {
		this.configFragment = configFragment;
		this.apiClient = apiClient;
		this.queries = queries;
	}

	/**
	 * The constructor to initialize the BulkApiQuery
	 */
	public BulkApiQueryEngine(SalesforceConfigFragment configFragment, BulkApiClient apiClient) {
		this(configFragment, apiClient, new LinkedList<>(List.of(configFragment.getBulkApiQueries().split(";"))));
	}

	/**
	 *
	 * @return a Stream of records
	 */
	public Stream<CSVRecord> getRecords() {

		for (String query : queries) {
			// Submit the job
			String jobId = apiClient.submitQueryJob(query);
			JobState state = apiClient.queryJobStatus(jobId);
			String locator="";
			// wait until the job is finished processing
			waitUntilProcessingComplete(state);
			switch(state){
			case UploadComplete:
				return apiClient.getResultStream(jobId, null);
			case Aborted:
			case Failed:
			default:
				apiClient.deleteJob(jobId);
				return null;
			}
		}
    return Stream.empty();
  }

	private void waitUntilProcessingComplete(JobState state) {
		while (state.equals(JobState.InProgress) || state.equals(JobState.Submitted) || state.equals(JobState.JobComplete))
		{
      try {
				// TODO Add a max wait time before returning and updating the state to failed
	      //e.g. wait for a max of 5 minutes for the job to process and then return fail if it isn't returned by then
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        LOGGER.error("Attempted to sleep until job was complete but an exception was thrown: ", e);
				throw new RuntimeException(e);
      }
    }
	}

}
