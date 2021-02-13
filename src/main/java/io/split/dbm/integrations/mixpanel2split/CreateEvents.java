package io.split.dbm.integrations.mixpanel2split;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;

public class CreateEvents {
	
	private String apiToken;
	private int batchSize;

	public CreateEvents(String splitApiToken, int batchSize) {
		this.apiToken = splitApiToken;
		this.batchSize = batchSize;
	}

	public void
	doPost(JSONArray events) throws Exception {
	    CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost("https://events.split.io/api/events/bulk");

	    int i = 0;
	    for( ; i < events.length();) {
	    	JSONArray batch = new JSONArray();
	    	int j = i;
	    	for( ; j < i + batchSize && j < events.length(); j++) {
	    		batch.put(events.getJSONObject(j));
	    	}	   
	    	System.out.println("INFO - sending events " + i + " ->  " + j + " of " + events.length());
	    	postToSplit(client, httpPost, batch);
	    	i += batchSize;
	    	Thread.sleep(1000);
	    }

	    client.close();
	}

	private void postToSplit(CloseableHttpClient client, HttpPost httpPost, JSONArray batch)
			throws UnsupportedEncodingException, IOException, ClientProtocolException {
		StringEntity entity = new StringEntity(batch.toString(2), Charset.forName("UTF-8"));
//		StringEntity entity = new StringEntity("[" + App.readFile("test.json") + "]", Charset.forName("UTF-8"));
		httpPost.setEntity(entity);
		httpPost.setHeader("Content-type", "application/json");
		String authorizationHeader = "Bearer " + apiToken;
		httpPost.setHeader("Authorization", authorizationHeader);
 
		CloseableHttpResponse response = client.execute(httpPost);
		System.out.println("INFO - POST to Split status code: " + response.getStatusLine());
		if(response.getStatusLine().getStatusCode() >= 400) {
			System.err.println(batch.getJSONObject(0).toString(2));
		}
		response.close();
	}
	
}
