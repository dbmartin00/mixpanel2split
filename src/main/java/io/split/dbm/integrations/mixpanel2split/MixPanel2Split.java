package io.split.dbm.integrations.mixpanel2split;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

public class MixPanel2Split {

	public void execute(String start, String end, final Configuration config) throws Exception {
		System.out.println("INFO - " + new JSONObject(new Gson().toJson(config)).toString(2));

//		OkHttpClient client = new OkHttpClient.Builder()
//				.authenticator(new Authenticator() {
//					public Request authenticate(Route route, Response response) throws IOException {
//						String credential = Credentials.basic(config.mixpanelProjectApiSecret, "");
//						return response.request().newBuilder().header("Authorization", credential).build();
//					}
//				})
//				.connectTimeout(config.connectTimeoutInSeconds, TimeUnit.SECONDS)
//				.readTimeout(config.readTimeoutInSeconds, TimeUnit.SECONDS)
//				.build();
//
//		String uri = "https://data.mixpanel.com/api/2.0/export?from_date=" + start + "&to_date=" + end;	
//		System.out.println("INFO - " + uri);
//		Request request = new Request.Builder()
//				.url(uri)
//				.build();
//
//		System.out.println("INFO - starting request... ");
//		Response response = client.newCall(request).execute();
//		System.out.println("success getting export?\t\t" + response.code());

		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(config.connectTimeoutInSeconds))
				.build();

		String apiUrl = "https://data.mixpanel.com/api/2.0/export?from_date=" + start + "&to_date=" + end;	
        URI uri = URI.create(apiUrl);
        HttpRequest request =  HttpRequest.newBuilder(uri).GET()
                .header("Authorization", basicAuth(config.mixpanelProjectApiSecret, ""))
                .build();
        System.out.printf("INFO - Requesting MixPanel events: GET %s %n", uri);

        // Process response
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if(response.statusCode() >= 300) {
            System.err.printf("ERROR - MixPanel events request failed: status=%s %n", response.statusCode());
            System.exit(1);
        }		
		
		long totalEventCount = 0;
		JSONArray rawEvents = new JSONArray();
		InputStream byteStream = response.body();
		BufferedReader reader = new BufferedReader(new InputStreamReader(byteStream));
		String line = null;
		while((line = reader.readLine()) != null) {
			rawEvents.put(new JSONObject(line));
			totalEventCount++;
			if(rawEvents.length() >= config.batchSize) {
				sendEventsToSplit(config, rawEvents);
				rawEvents = new JSONArray();
				System.out.println("INFO - " + totalEventCount + " event sent");
			}			
		}	
		if(rawEvents.length() > 0) {
			sendEventsToSplit(config, rawEvents);
		}
		System.out.println("INFO - " + totalEventCount + " events sent");
	}

	private void sendEventsToSplit(final Configuration config, JSONArray rawEvents) throws Exception {
		JSONArray splitEvents = new JSONArray();
		for(int i = 0; i < rawEvents.length(); i++) {
			JSONObject rawEvent = rawEvents.getJSONObject(i);
			JSONObject rawProperties = rawEvent.getJSONObject("properties");

			for(Mapping mapping : config.mappings) {
				String key = null;
				if(rawProperties.has(mapping.key)) {
					key = rawProperties.getString(mapping.key);
				}
				if(key != null && !key.isEmpty()) {
					JSONObject splitEvent = new JSONObject();
					splitEvent.put("key", key);
					splitEvent.put("trafficTypeName", mapping.trafficType);
					splitEvent.put("eventTypeId", cleanEventTypeId(rawEvent.getString("event")));
					splitEvent.put("value", rawProperties.has("value") ? rawProperties.get("value") : config.value);
					splitEvent.put("environmentName", config.environment);
					splitEvent.put("timestamp", Long.parseLong(("" + rawProperties.getLong("time")) + "000" ));
					Map<String, Object> properties = new TreeMap<String, Object>();
					putProperties(properties, config.eventPrefix, rawProperties);
					splitEvent.put("properties", properties);

					splitEvents.put(splitEvent);
				}
			}
		}

		CreateEvents creator = new CreateEvents(config.splitServerSideApiKey, config.batchSize);
		creator.doPost(splitEvents);	
	}

	private String cleanEventTypeId(String eventName) {
		String result = "";

		char letter;
		for(int i = 0; i < eventName.length(); i++) {
			letter = eventName.charAt(i);
			if(!Character.isAlphabetic(letter)
					&& !Character.isDigit(letter)) {
				if(i == 0) {
					letter = '0';
				} else {
					if (letter != '-' && letter != '_' && letter != '.') {
						letter = '_';
					}
				}
			}
			result += "" + letter;
		}

		return result;
	}

	public static String readFile(String path)
			throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, Charset.defaultCharset());
	}

	private void putProperties(Map<String, Object> properties, String prefix, JSONObject obj) {
		for(String k : obj.keySet()) {
			if(!(obj.get(k) instanceof JSONArray)) {
				properties.put(prefix + k, obj.get(k));
			} else {
				JSONArray array = obj.getJSONArray(k);
				properties.put(prefix + k, array.toString());
			}
		}
	}
	
    private static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
