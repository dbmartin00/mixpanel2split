package io.split.dbm.integrations.mixpanel2split;

import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;

public class CreateEvents {

	private static final String SPLIT_EVENTS_URL = "https://events.split.io/api/events/bulk";

	private String apiToken;
	private int batchSize;
	private final HttpClient httpClient;
	private final Configuration config;
	
	public CreateEvents(String splitApiToken, Configuration config) {
		this.apiToken = splitApiToken;
		this.batchSize = config.batchSize;
		this.httpClient = HttpClient.newHttpClient();
		this.config = config;
	}

	public void
	doPost(JSONArray events) throws Exception {
		int i = 0;
		for( ; i < events.length();) {
			JSONArray batch = new JSONArray();
			int j = i;
			for( ; j < i + batchSize && j < events.length(); j++) {
				batch.put(events.getJSONObject(j));
			}	   
			System.out.println("INFO - sending events " + i + " ->  " + j + " of " + events.length());
			postToSplit(batch);
			i += batchSize;
		}
	}

	private void postToSplit(JSONArray batch){
		try {
			System.out.println("INFO - Sending batch of events: size=" + batch.length());

			// Build Request
			HttpRequest request = HttpRequest.newBuilder(URI.create(SPLIT_EVENTS_URL))
					.header("Content-type", "application/json")
					.header("Authorization", "Bearer " + apiToken)
					.POST(HttpRequest.BodyPublishers.ofString(batch.toString()))
					.build();

			// Process Response
			HttpResponse<String> response;
			int retries = 0;
			int statusCode = -1;
			do {
				response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				statusCode = response.statusCode();
				if(response.statusCode() >= 400) {
					System.err.printf("ERROR - Sending events to Split failed: status=%s response=%s %n", response.statusCode(), response.body());
				}
			} while(retries++ < config.retries && response != null && response.statusCode() >= 400);

			if(statusCode >= 400 && response != null && response.body() != null) {
				PrintWriter out = new PrintWriter(config.debugDirectory + System.getProperty("file.separator") + "mixpanel2split-debug-" + System.currentTimeMillis() + ".json");
				out.println(batch.toString(2));
				out.close();
			}
			
			// Courtesy to minimize pressure on API
			Thread.sleep(100);
		} catch(Exception e) {
			System.err.println("ERROR - Failed to send event");
			e.printStackTrace(System.err);
		}
	}

}
