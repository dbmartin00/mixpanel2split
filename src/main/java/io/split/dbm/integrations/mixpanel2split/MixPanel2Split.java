package io.split.dbm.integrations.mixpanel2split;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

public class MixPanel2Split {

	private static Logger LOGGER;

	static {
	      Logger mainLogger = Logger.getLogger(MixPanel2Split.class.getName());
	      mainLogger.setUseParentHandlers(false);
	      ConsoleHandler handler = new ConsoleHandler();
	      handler.setFormatter(new SimpleFormatter() {
	          private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

	          @Override
	          public synchronized String format(LogRecord lr) {
	              return String.format(format,
	                      new Date(lr.getMillis()),
	                      lr.getLevel().getLocalizedName(),
	                      lr.getMessage()
	              );
	          }
	      });
	      mainLogger.addHandler(handler);
	      LOGGER = mainLogger;
	}
	
	public MixPanel2Split() {

	}

	public void execute(String start, String end, final Configuration config) throws Exception {
		LOGGER.log(Level.INFO, new JSONObject(new Gson().toJson(config)).toString(2));

		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(config.connectTimeoutInSeconds))
				.build();

		String events = toCommaSeparatedString(config.eventNames);
		String encodedEvents = URLEncoder.encode(events, StandardCharsets.UTF_8.name());
		String apiUrl = "https://data.mixpanel.com/api/2.0/export?from_date=" + start + "&to_date=" + end + "&event=[" + encodedEvents + "]";
		URI uri = URI.create(apiUrl);
		HttpRequest request =  HttpRequest.newBuilder(uri).GET()
				.header("Authorization", basicAuth(config.mixpanelProjectApiSecret, ""))
				.timeout(Duration.ofSeconds(config.connectTimeoutInSeconds))
				.build();
		LOGGER.log(Level.INFO, "Requesting MixPanel events: GET " + uri);

		// Process response
		long startMP = System.currentTimeMillis();
		HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
		if(response.statusCode() >= 300) {
			LOGGER.log(Level.SEVERE, "MixPanel events API request failed: status=" + response.statusCode() + " body=" + response.body());
			System.exit(1);
		}		
		LOGGER.log(Level.INFO, "MixPanel API responded in " + ((System.currentTimeMillis() - startMP) / 1000) + "s");

		JSONArray rawEvents = new JSONArray();
		InputStream byteStream = response.body();
		BufferedReader reader = new BufferedReader(new InputStreamReader(byteStream));
		int retries = 0;
		try {
			readLoop(reader, rawEvents, config);
		} catch (Exception ex) {
			while(retries++ < 5) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				Thread.sleep(500);
				try {
					readLoop(reader, rawEvents, config);
					break;
				} catch(Exception ex2) {
					LOGGER.log(Level.SEVERE, ex2.getMessage(), ex2);
				}
			}
		}

		if(rawEvents.length() > 0) {
			// LOGGER.log(Level.INFO, "event to post = " + rawEvents.length());
			EventCounter.mixpanelEventQuery += rawEvents.length();
			sendEventsToSplit(config, rawEvents);
		}

		LOGGER.log(Level.INFO, "Total Event Get from Mixpanel = " + EventCounter.mixpanelEventQuery);
		LOGGER.log(Level.INFO, "Total Event Sent to Split = " + EventCounter.splitEventSent);
	}

	private String toCommaSeparatedString(List<String> strings) {
		String result = "";
		if(strings != null && !strings.isEmpty()) {
			int i = 0;

			for(String event : strings){

				if(i++ < strings.size() - 1){
					result += "\"" + event + "\",";
				} else {
					result += "\"" + event + "\"";
				}
			}
		}
		return result;
	}

	private void readLoop(BufferedReader reader, JSONArray rawEvents, Configuration config) throws Exception {
		String line = null;
		while((line = reader.readLine()) != null) {
			rawEvents.put(new JSONObject(line));
			if(rawEvents.length() >= config.batchSize) {
				EventCounter.mixpanelEventQuery += rawEvents.length();
				sendEventsToSplit(config, rawEvents);
				rawEvents = new JSONArray();
			}			
		}	

	}


	private void sendEventsToSplit(final Configuration config, JSONArray rawEvents) throws Exception {
		JSONArray splitEvents = new JSONArray();
		for(int i = 0; i < rawEvents.length(); i++) {
			JSONObject rawEvent = rawEvents.getJSONObject(i);
			JSONObject rawProperties = rawEvent.getJSONObject("properties");
			JSONObject splitEvent = new JSONObject();

			for(Mapping mapping : config.mappings) {
				String key = null;
				if(rawProperties.has(mapping.key)) {
					key = rawProperties.getString(mapping.key);
				}
				if(key != null && !key.isEmpty()) {
					splitEvent.put("key", key);
					splitEvent.put("trafficTypeName", mapping.trafficType);
					splitEvent.put("eventTypeId", cleanEventTypeId(rawEvent.getString("event")));
					splitEvent.put("value", rawProperties.has("value") ? rawProperties.get("value") : config.value);
					splitEvent.put("environmentName", config.environment);
					splitEvent.put("timestamp", Long.parseLong(("" + rawProperties.getLong("time")) + "000" ));
					Map<String, Object> properties = new TreeMap<String, Object>();
					putProperties(properties, config.eventPrefix, rawProperties);
					splitEvent.put("properties", properties);
				}
			}

			splitEvents.put(splitEvent);
		}

		final JSONArray batchToPost = splitEvents;
		CreateEvents creator = new CreateEvents(config.splitServerSideApiKey, config);
		creator.doPost(batchToPost);	
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
