package io.split.dbm.integrations.mixpanel2split;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.google.gson.Gson;

public class Configuration {

	public int lastNumberOfDays;
	public int connectTimeoutInSeconds;
	public int readTimeoutInSeconds;
	public List<String> eventNames;
	public String mixpanelProjectApiSecret;
	public String splitServerSideApiKey;
	public String environment;
	public String eventPrefix;
	public String[] includedEvents;
	public Mapping[] mappings; 
	public String key;
	public long value;
	public int batchSize;   
	public int retries;
	public String debugDirectory;
	public int eventPostingThreadCount;

	public static Configuration fromFile(String configFilePath) throws IOException {
		String configContents = Files.readString(Paths.get(configFilePath));
		return new Gson().fromJson(configContents, Configuration.class);
	}
}