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

import org.json.JSONObject;

public class App 
{
    public static void main( String[] args ) throws Exception
    {  
		if(args.length < 1) {
			System.err.println("ERROR - first argument should be configuration file  (e.g. java -jar mixpanel2split.jar mixpanel2split.config)");
		} else {
			File configFile = new File(args[0]);
			if(!configFile.exists()) {
				System.err.println("ERROR - file doesn't exist: " + args[0]);		
			} else {
				try {
					new JSONObject(readFile(args[0]));
					new App().execute(args[0]);
				} catch (Exception e) {
					System.err.println("ERROR - invalid JSON config file: " + args[0]);			
				}
			}
		}
    }
   
	private void execute(String configFilePath) {
		long begin = System.currentTimeMillis();

		try {
			String configFile = readFile(configFilePath);
			JSONObject configObj = new JSONObject(configFile);

			Instant nowUtc = Instant.now();
			Instant daysAgoUtc = nowUtc.minus(configObj.getInt("lastNumberOfDays"), ChronoUnit.DAYS);
			Date now = Date.from(nowUtc);
			Date daysAgo = Date.from(daysAgoUtc);

			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

			String start = format.format(daysAgo);
			String end = format.format(now);

			System.out.println("INFO - reporting from " + start + " to " + end);
			
			new MixPanel2Split().execute(start, end, configObj);
			
		} catch(Exception e) {
			System.err.println("ERROR - exiting with error: " + e.getMessage());
			e.printStackTrace(System.err);
		} finally {
			System.out.println("INFO - finish in " + ((System.currentTimeMillis() - begin) / 1000) + "s");			
		}
	}

	public static String readFile(String path)
			throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, Charset.defaultCharset());
	}
}
