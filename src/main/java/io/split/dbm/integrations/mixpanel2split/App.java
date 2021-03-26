package io.split.dbm.integrations.mixpanel2split;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

public class App 
{
	private final static Logger LOGGER = Logger.getLogger(App.class.getName());
	
    public static void main( String[] args ) throws Exception
    {  
		if(args.length < 1) {
			LOGGER.log(Level.SEVERE, "first argument should be configuration file  (e.g. java -jar mixpanel2split.jar mixpanel2split.config)");
		} else {
			File configFile = new File(args[0]);
			if(!configFile.exists()) {
				LOGGER.log(Level.SEVERE, "file doesn't exist: " + args[0]);		
			} else {
				try {
					new JSONObject(readFile(args[0]));
					new App().execute(args[0]);
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "invalid JSON config file: " + args[0]);			
				}
			}
		}
    }
   
	private void execute(String configFilePath) {
		long begin = System.currentTimeMillis();

		try {
			Configuration config = Configuration.fromFile(configFilePath);

			Instant nowUtc = Instant.now();
			Instant daysAgoUtc = nowUtc.minus(config.lastNumberOfDays, ChronoUnit.DAYS);
			Date now = Date.from(nowUtc);
			Date daysAgo = Date.from(daysAgoUtc);

			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

			String start = format.format(daysAgo);
			String end = format.format(now);

			LOGGER.log(Level.INFO, "reporting from " + start + " to " + end);
			
			new MixPanel2Split().execute(start, end, config);
			
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "exiting with error: " + e.getMessage(), e);
		} finally {
			LOGGER.log(Level.INFO, "finished in " + ((System.currentTimeMillis() - begin) / 1000) + "s");			
		}
	}

	public static String readFile(String path)
			throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, Charset.defaultCharset());
	}
}
