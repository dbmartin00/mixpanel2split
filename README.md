# MixPanel to Split Events Integration

![alt text](http://www.cortazar-split.com/MixPanel2Split.png)

To run, build the executable JAR file and run with a JSON configuration as argument.

Compile with Maven:

mvn clean compile assembly:single

Executable JAR takes name of configuration file as argument.

Sample configuration file.
```
{
  "lastNumberOfDays" : 1,
  "connectTimeoutInSeconds" : 1800,
  "readTimeoutInSeconds" : 1800,
  "mixpanelProjectApiSecret" : "SECRET"
  "splitServerSideApiKey" : "SECRET",
  "environment" : "Prod-Default",
  "eventPrefix" : "mix.",
  "includedEvents" : [],
  "mappings" : [
  	  {
        "trafficType": "user",
        "key": "$user_id"
      },
      {
        "trafficType": "device",
        "key": "UTDID"
      },
      {
        "trafficType": "user",
        "key": "distinct_id"
  	  }
  ],  
  "value" : 0,
  "batchSize" : 500  
}
```
Configuration Fields:

* "lastNumberOfDays" - how many days of MixPanel events should be extracted, counting backwards from today?
* "connectTimeoutInSeconds" - lengthy delay allowed by default to be friendly to MixPanel backend
* "readTimeoutInSeconds" - lengthy timeout allowed by default to be friendly to MixPanel backend
* "mixpanelProjectApiSecret" - find it in your project settings; must be API secret
* "splitServerSideApiKey" - Split server-side SDK key 
* "environment" - Split environment, often "Prod-Default" or as found in Split UI
* "eventPrefix" - how will MixPanel properties be prefixed in Split?
* "includedEvents" - not yet implemented; all events are included
* "mapping" - events are expected for a key; when key is present and non-empty the event is sent to Split with the paired traffic type.  Events can match more than one key, but if they don't match any key they are never sent.
* "batchSize" - how many Split events to send across in a single API request
* "value" - leave at zero in most cases
