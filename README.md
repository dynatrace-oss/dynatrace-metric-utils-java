# dynatrace-metric-utils-java

Java Utility for preparing communication with the [Dynatrace v2 metrics API](https://www.dynatrace.com/support/help/dynatrace-api/environment-api/metric-v2/).

## Installation

The library is available on [Maven Central](https://mvnrepository.com/artifact/com.dynatrace.metric.util/dynatrace-metric-utils-java/latest)
(`com.dynatrace.metric.util:dynatrace-metric-utils-java`), where snippets for major dependency managers can be found.

## Usage

Examples for how to use this library can be found in [the example application](example/src/main/java/com/dynatrace/metric/util/example/App.java).

It shows how to create metrics lines that can be sent to a [Dynatrace metrics ingest endpoint](https://www.dynatrace.com/support/help/dynatrace-api/environment-api/metric-v2/post-ingest-metrics/).

### Metric line creation

Metric lines can be created by using the `MetricLineBuilder` interface.
The `MetricLineBuilder` allows for setting all required data to create metric lines and metadata lines.
Once all required data is set, `build()` will create a string containing the serialized line with all normalized data.
Upon creation of the `MetricLineBuilder`, it is possible to pass a `MetricLinePreConfiguration` object that contains data that is the same between multiple `MetricLineBuilders` (e.g. default dimensions, prefix).

#### MetricLinePreConfiguration

The standard workflow consists of creating a `MetricLinePreConfiguration` and passing it during creation of a `MetricLineBuilder`.
The following settings can be set upon `MetricLinePreConfiguration` construction:  

* `prefix`: A prefix that is prepended to each metric key (separated by a `.`).
* `defaultDimensions`: Default dimensions that will be added to every metric using this `MetricLinePreConfiguration` object (see [the section on dimension precedence](#dimension-precedence) below).
* `dynatraceMetadataDimensions`: With this setting enabled, the library will connect to the Dynatrace OneAgent, if available, and retrieve process and host identifiers. 
  These identifiers are added as dimensions on all metrics to correlate them accordingly.
  More information on the underlying feature can be found in the
  [Dynatrace documentation](https://www.dynatrace.com/support/help/how-to-use-dynatrace/metrics/metric-ingestion/ingestion-methods/enrich-metrics/).
  If running in a containerized environment with a compatible Dynatrace operator present, additional metadata about the container environment may be added.

```java
Map<String, String> defaultDims = new HashMap<>();
defaultDims.put("a.key", "a.value");

MetricLinePreConfiguration preConfig =
    MetricLinePreConfiguration.builder()
        .defaultDimensions(defaultDims)
        .dynatraceMetadataDimensions()
        .prefix("prefix")
        .build();
```

This pre-configuration can then be used to create `MetricLineBuilder` instance, which represent one data point before serialization.
If the pre-configuration is omitted, **no** Dynatrace metadata, default dimensions or prefix will be set on the new metric.
To create metric lines from data points, the `MetricLineBuilder` can be used as follows:

```java
// single-valued gauge
MetricLineBuilder.create(preConfig)   // the (optional) MetricLinePreConfiguration
        .metricKey("my_gauge")        // the metric key of the metric
        .dimensions(dimensions)       // set dynamic dimensions that are specific to the current metric
        .gauge()                      // set the type of the current metric
        .value(123)                   // set the value of the current metric
        .timestamp(Instant.now())     // set a timestamp for the current metric
        .build();                     // create a String from the information set above.

// summary gauge
MetricLineBuilder.create(preConfig)
        .metricKey("my_summary")
        .dimensions(dimensions)
        .gauge()
        .summary(1, 2, 3, 2)
        .timestamp(Instant.now())
        .build();

// counter
MetricLineBuilder.create(preConfig)
        .metricKey("my_count")
        .dimensions(dimensions)
        .count()
        .delta(4)
        .timestamp(Instant.now())
        .build();
```

#### Metric line creation

1. `create`: Instantiates a `MetricLineBuilder` with an optional `MetricLinePreConfiguration`.
2. `metricKey`: Sets and normalizes the metric key of the metric line.
3. `dimensions` / `dimension`:  Sets one or more dimensions specific to this metric.
   * When using a metric line pre-configuration: Default dimensions and Dynatrace metadata will be merged with the newly set dimension(s). See [the section on dimension precedence](#dimension-precedence) below.
   * When setting dimensions with the same dimension key multiple times, the dimension value that was set last is used for that dimension key.
4. `count` / `gauge`: Sets the type of the metric line.
  Use `gauge()` for gauges and summary statistics, and `count()` for counters.
5. Sets the value:
   * (`count()` only): `delta` sets a single value that is serialized as `delta=<value>`.
   * (`gauge()` only): `value` sets a single value that is serialized as `<value>`.
   * (`gauge()` only): `summary` sets min, max, sum and count values that are serialized as `min=<min>,max=<max>,sum=<sum>,count=<count>`.
6. (optional): `timestamp`: Sets a specific `Instant` object on the metric that will be used to create the timestamp on the metric line.
7. `build`: Serializes the metric data and returns the complete metric line as a string.

### Metadata line creation

The `MetricLineBuilder` can also be used to serialize metadata information.
After setting the metric key and type on a `MetricLineBuilder`, use the `.metadata()` method to create a `MetadataLineBuilder`.
To create metadata lines from data points, the `MetadataLineBuilder` can be used as follows:

```java
MetricLineBuilder.create(preConfig)     // the (optional) MetricLinePreConfiguration
        .metricKey("my_gauge")          // the metric key of the current metric
        .gauge()                        // set the type of the current metric
        .metadata()                     // start the MetadataBuilder
        .unit("unit")                   // set the unit for the current metric
        .description("my description")  // set the description for the current metric
        .displayName("my display name") // set the display name for the current metric
        .build();                       // create a String from the information set above
```
#### Metadata line creation

1. Create a `MetricLineBuilder` with a metric key and a type.
2. `metadata()`: Creates a `MetadataLineBuilder` with the metric key and type set above.
  `MetricLineBuilder`s can be reused (e.g. for creating the metric line and the metadata line).
  It is not required to create separate `MetricLineBuilder` instances.
3. `description` / `unit` / `displayName`:
   *  `description` Sets the description that is serialized as `dt.meta.description=<description>`.
   *  `unit` Sets the unit that is serialized as `dt.meta.unit=<unit>`.
   *  `displayName` Sets the display name that are serialized as `dt.meta.displayName=<displayName>`.
4. `build`: Serializes the metadata information and returns the metadata line as a String.

To produce a valid metadata line, at least one property has to be set.
If none are set, the returned metadata line will be `null`.
The order in which metadata is set does not matter.

#### Dimension precedence

Since there are multiple levels of dimensions (default, dynamic, Dynatrace metadata) and duplicate keys are not allowed, there is a specified precedence in dimension keys.
Default dimensions will be overwritten by dynamic dimensions, and all dimensions will be overwritten by Dynatrace metadata dimensions if they share the same key after normalization.
Note that the Dynatrace metadata dimensions will only contain [dimension keys reserved by Dynatrace](https://www.dynatrace.com/support/help/how-to-use-dynatrace/metrics/metric-ingestion/metric-ingestion-protocol/#syntax).
If the `.dynatraceMetadataDimensions()` method is not called on the `MetricLinePreConfiguration`, Dynatrace metadata will not be added.

### Common constants

The library also provides constants that might be helpful in the projects consuming this library.

To access the constants, call the respective static methods on `DynatraceMetricApiConstants`:

```java
String oneAgentEndpoint = DynatraceMetricApiConstants.getDefaultOneAgentEndpoint();
```

Currently available constants are:

* the default [local OneAgent metric API](https://www.dynatrace.com/support/help/how-to-use-dynatrace/metrics/metric-ingestion/ingestion-methods/local-api/) endpoint (`getDefaultOneAgentEndpoint()`)
* the limit for how many metric lines can be ingested in one request (`getPayloadLinesLimit()`)
