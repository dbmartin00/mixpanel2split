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
  "mixpanel.project.api.secret" : "YOUR MIXPANEL API SECRET",
  "split.admin.api.key" : "YOUR SPLIT SERVER-SIDE SDK KEY",
  "trafficType" : "user",
  "environment" : "Prod-Default",
  "eventPrefix" : "mix.",
  "key" : "distinct_id",
  "value" : "",
  "batchSize" : 5000  
}
```
Configuration Fields:

* "lastNumberOfDays" - how many days of MixPanel events should be extracted, counting backwards from today?
* "connectTimeoutInSeconds" - lengthy delay allowed by default to be friendly to MixPanel backend
* "readTimeoutInSeconds" - lengthy timeout allowed by default to be friendly to MixPanel backend
* "mixpanel.project.api.secret" - find it in your project settings; must be API secret
* "split.admin.api.key" - should be the Split server-side SDK key (despite the naming)
* "trafficType" - Split event traffic type, often "user" or "anonymous"
* "environment" - Split environment, often "Prod-Default" or as found in Split UI
* "key" - will use "distinct_id" as the unique key from MixPanel events by default
* "batchSize" - how many Split events to send across in a single API request

