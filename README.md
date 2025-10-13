# CoralConfig
CoralConfig is a configuration framework that offers a fluent API, aliases, deprecation support, default values, _strongly typed_ configuration types, and more. It provides a robust solution to manage the many configuration parameters and their types that applications typically require.

## Examples
```java
public class Basics {
	
	public static final ConfigKey<Integer> MAX_NUMBER_OF_RETRIES = intKey().def(4); // intKey(4) also works
	
	private final int maxNumberOfRetries;
	
	public Basics(Configuration config) {
		this.maxNumberOfRetries = config.get(MAX_NUMBER_OF_RETRIES);
		System.out.println(MAX_NUMBER_OF_RETRIES + " => " + maxNumberOfRetries);
	}
}
```
To create an empty configuration:
```java
MapConfiguration mc = new MapConfiguration(Basics.class);
```
To get the default value:
```java
int maxNumberOfRetries = mc.get(MAX_NUMBER_OF_RETRIES); // => 4 (the default)
```
To overwrite a default value:
```java
mc.overwriteDefault(MAX_NUMBER_OF_RETRIES, 6);

int maxNumberOfRetries = mc.get(MAX_NUMBER_OF_RETRIES); // => 6 (overwritten default)
```
To add a value so it is returned instead of the default or any overwritten default value:
```java
mc.add(MAX_NUMBER_OF_RETRIES, 2);

int maxNumberOfRetries = mc.get(MAX_NUMBER_OF_RETRIES); // => 2 (configured)
```
To create a configuration with some values you can pass a list of params to the constructor:
```java
MapConfiguration mc = new MapConfiguration("maxNumberOfRetries=1 username=saoj heartbeat=30",
													Basics.class);

int maxNumberOfRetries = mc.get(MAX_NUMBER_OF_RETRIES); // => 1
```

### Supports more than one _holder_ class
The configuration can handle multiple _holder_ classes with distinct configuration keys. It enforces that there are no duplicate keys.
```java
public class Client {
    public static final ConfigKey<Integer> HEARTBEAT = intKey().def(5);
}

public class TcpClient extends Client {
    public static final ConfigKey<String> USERNAME = stringKey().def("saoj");
}
```
Simply pass a list of _holder_ classes:
```java
MapConfiguration mc = new MapConfiguration("heartbeat=2 username=rpaiva",
												Client.class, TcpClient.class);
```

### _Strongly typed_ configuration types
If you get the type of the config key wrong, it won't even compile:
```java
 // DOESN'T COMPILE (wrong type)
String maxNumberOfRetries = mc.get(MAX_NUMBER_OF_RETRIES);
```

### Supports _aliases_
Don't like the name of a configuration key? Simply create an alias:
```java
public class Client {
    public static final ConfigKey<Integer> HEARTBEAT_INTERVAL = intKey().def(5);
    public static final ConfigKey<Integer> HEARTBEAT = intKey().alias(HEARTBEAT_INTERVAL);
}
```
We did not like the name `heartbeat` so we created `heartbeatInterval` as the primary config key and added `heartbeat` as an alias for backwards compatibility. Now we can do:
```java
MapConfiguration mc = new MapConfiguration(Client.class);
int heartbeatInterval = mc.get(HEARTBEAT_INTERVAL); // => 5
// This also works with the default value of the primary config key!
int heartbeat = mc.get(HEARTBEAT); // => 5
```
And if you declare a value for the new field `heartbeatInterval` then the primary key together with all its aliases will get the declared value:
```java
mc.add(HEARTBEAT_INTERVAL, 2);
int heartbeatInterval = mc.get(HEARTBEAT_INTERVAL); // => 2
// The alias also gets the declared value!
int heartbeat = mc.get(HEARTBEAT); // => 2
```
or if you do this instead:
```java
mc.add(HEARTBEAT, 2);
// The primary key gets the value declared for its alias!
int heartbeatInterval = mc.get(HEARTBEAT_INTERVAL); // => 2
int heartbeat = mc.get(HEARTBEAT); // => 2
```
You can have as many aliases as you want:
```java
public class Client {
    public static final ConfigKey<Integer> HEARTBEAT_INTERVAL = intKey().def(5);
    public static final ConfigKey<Integer> HEARTBEAT = intKey().alias(HEARTBEAT_INTERVAL);
    public static final ConfigKey<Integer> HEARTBEAT_TIME = intKey().alias(HEARTBEAT_INTERVAL);
}
```
