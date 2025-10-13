# CoralConfig
CoralConfig is a configuration framework that offers a fluent API, aliases, deprecation support, default values, _strongly typed_ configuration types (including enum), and more. It provides a robust solution to manage the many configuration parameters and their types that applications typically require.

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
To configure (add) a value so this value is returned instead of the (overwritten or not) default value:
```java
mc.add(MAX_NUMBER_OF_RETRIES, 2);

int maxNumberOfRetries = mc.get(MAX_NUMBER_OF_RETRIES); // => 2 (configured)
```
To create a configuration with some values, you can pass a list of params to the constructor:
```java
MapConfiguration mc = new MapConfiguration("maxNumberOfRetries=1 username=saoj heartbeat=30",
													Basics.class);

int maxNumberOfRetries = mc.get(MAX_NUMBER_OF_RETRIES); // => 1 (configured)
```

### Supports more than one _holder_ class
The configuration can handle multiple _holder_ classes with distinct configuration keys. The configuration enforces that there are no duplicate keys in each _holder_ class as well as across all _holder_ classes.
```java
public class Client {
    public static final ConfigKey<Integer> HEARTBEAT = intKey(5);
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
int heartbeatInterval = mc.get(HEARTBEAT_INTERVAL); // => 5 (default)
// This also works with the default value of the primary config key!
int heartbeat = mc.get(HEARTBEAT); // => 5 (default)
```
And if you declare a value for the new config key `heartbeatInterval` then the primary key together with all its aliases will get the declared value:
```java
mc.add(HEARTBEAT_INTERVAL, 2);
int heartbeatInterval = mc.get(HEARTBEAT_INTERVAL); // => 2 (configured)
// The alias also gets the declared value!
int heartbeat = mc.get(HEARTBEAT); // => 2 (configured)
```
or if you do this instead:
```java
mc.add(HEARTBEAT, 2);
// The primary key gets the value declared for its alias!
int heartbeatInterval = mc.get(HEARTBEAT_INTERVAL); // => 2 (configured)
int heartbeat = mc.get(HEARTBEAT); // => 2 (configured)
```
You can have as many _aliases_ as you want:
```java
public class Client {
    public static final ConfigKey<Integer> HEARTBEAT_INTERVAL = intKey().def(5);
    public static final ConfigKey<Integer> HEARTBEAT = intKey().alias(HEARTBEAT_INTERVAL);
    public static final ConfigKey<Integer> HEARTBEAT_TIME = intKey().alias(HEARTBEAT_INTERVAL);
}
```

### Supports _deprecation_
Chose a bad name for a configuration? Simply deprecate it:
```java
public class Client {
    public static final ConfigKey<Integer> HEARTBEAT_INTERVAL = intKey().def(5);
    public static final ConfigKey<Integer> HEARTBEAT = intKey().deprecated(HEARTBEAT_INTERVAL);
}
```
We chose a bad name by mistake (`heartbeat`) so we created `heartbeatInterval` as the primary config key and added `heartbeat` as deprecated for backwards compatibility. Now we can do:
```java
MapConfiguration mc = new MapConfiguration(Client.class);
int heartbeatInterval = mc.get(HEARTBEAT_INTERVAL); // => 5 (default)
// This also works with the default value of the primary config key!
int heartbeat = mc.get(HEARTBEAT); // => 5 (default)
```
And if you declare a value for the new config key `heartbeatInterval` then the primary key together with all its deprecated keys will get the declared value:
```java
mc.add(HEARTBEAT_INTERVAL, 2);
int heartbeatInterval = mc.get(HEARTBEAT_INTERVAL); // => 2 (configured)
// The deprecated key also gets the declared value!
int heartbeat = mc.get(HEARTBEAT); // => 2 (configured)
```
or if you do this instead:
```java
mc.add(HEARTBEAT, 2);
// The primary key gets the value declared for its deprecated keys!
int heartbeatInterval = mc.get(HEARTBEAT_INTERVAL); // => 2 (configured)
int heartbeat = mc.get(HEARTBEAT); // => 2 (configured)
```
You can have as many _deprecated_ configuration keys as you want:
```java
public class Client {
    public static final ConfigKey<Integer> HEARTBEAT_INTERVAL = intKey().def(5);
    public static final ConfigKey<Integer> HEARTBEAT = intKey().deprecated(HEARTBEAT_INTERVAL);
    public static final ConfigKey<Integer> HEARTBEAT_TIME = intKey().deprecated(HEARTBEAT_INTERVAL);
}
```
And finally, if you want to know the _deprecated_ configuration keys that you are still using, you can add a `DeprecatedListener` to your configuration. Below a full example:
```java
public class DeprecationBasics {
	
	public static final ConfigKey<Integer> MAX_NUMBER_OF_RETRIES = intKey().def(4);
	public static final ConfigKey<Integer> MAX_RETRIES = intKey().deprecated(MAX_NUMBER_OF_RETRIES);
	
	private final int maxRetries;
	
	public DeprecationBasics(Configuration config) {
		this.maxRetries = config.get(MAX_RETRIES); // using deprecated key!
	}
	
	public static void main(String[] args) {
		
		MapConfiguration config = new MapConfiguration(DeprecationBasics.class);

		DeprecatedListener listener = new DeprecatedListener() {
			
			@Override
			public void deprecatedConfig(ConfigKey<?> deprecatedKey, ConfigKey<?> primaryKey) {
				DeprecatedListener.super.deprecatedConfig(deprecatedKey, primaryKey);
			}
		};
		
		config.addListener(listener);
		
		new DeprecationBasics(config);
	}
}
```
When you run the program above you see in the stdout:
<pre>
---CoralConfig---> You are using a deprecated config key!
	holder=com.coralblocks.coralconfig.example.DeprecationBasics
	deprecatedKey=MAX_RETRIES("maxRetries") 
	inFavorOf=MAX_NUMBER_OF_RETRIES("maxNumberOfRetries")
</pre>

### Supports _Enums_:
```java
@Test
public void testEnum() {
	
	enum TestEnum { BALL, BOB, BILL }
	
	class Base {
		public static final ConfigKey<TestEnum> MY_ENUM = enumKey(TestEnum.class, TestEnum.BOB);
	}
	
	MapConfiguration mc = new MapConfiguration(Base.class);
	
	assertEquals(TestEnum.BOB, mc.get(Base.MY_ENUM)); // test default
	
	mc.overwriteDefault(Base.MY_ENUM, TestEnum.BILL);
	
	assertEquals(TestEnum.BILL, mc.get(Base.MY_ENUM)); // test overwritten default
	
	mc.add(Base.MY_ENUM, TestEnum.BALL);
	
	assertEquals(TestEnum.BALL, mc.get(Base.MY_ENUM)); // test configured
	
	mc.remove(Base.MY_ENUM);
	
	assertEquals(TestEnum.BILL, mc.get(Base.MY_ENUM)); // test overwritten default
	
	mc.overwriteDefault(Base.MY_ENUM, null); // String and Enum support NULL !!!
	
	assertEquals(null, mc.get(Base.MY_ENUM)); // test overwritten default
	
	mc.removeOverwrittenDefault(Base.MY_ENUM);
	
	assertEquals(TestEnum.BOB, mc.get(Base.MY_ENUM)); // test default
}
```
