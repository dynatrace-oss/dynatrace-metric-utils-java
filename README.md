# dynatrace-metric-utils-java

Java Utility for preparing communication with the [Dynatrace v2 metrics API](https://www.dynatrace.com/support/help/dynatrace-api/environment-api/metric-v2/).

## Installation

The library is available on [Maven Central](https://mvnrepository.com/artifact/com.dynatrace.metric.util/dynatrace-metric-utils-java/latest)
(`com.dynatrace.metric.util:dynatrace-metric-utils-java`), where snippets for major dependency managers can be found.

## Usage

Examples for how to use this library can be found in [the example application](example/src/main/java/com/dynatrace/example/App.java).
It shows how to create metrics lines that can be sent to a [Dynatrace metrics ingest endpoint](https://www.dynatrace.com/support/help/dynatrace-api/environment-api/metric-v2/post-ingest-metrics/).

### Metric line creation

#### MetricBuilderFactory

The standard workflow consists of creating a `MetricBuilderFactory` and using it to create `MetricBuilder` objects that can be serialized to a `String`.

The following options can be set on upon creation of a `MetricBuilderFactory`:

* `withPrefix`: A prefix that is prepended to each metric key (separated by a `.`).
* `withDefaultDimensions`: These dimensions will be added to all metrics created by the factory (see [the section on dimension precedence](#dimension-precedence) below).
* `withDynatraceMetadata`: With this setting enabled, the library will connect to the Dynatrace OneAgent, if installed,
  and retrieve process and host identifiers that are added as dimensions on all metrics to correlate them accordingly.
  More information on the underlying feature that is used by the library can be found in the
  [Dynatrace documentation](https://www.dynatrace.com/support/help/how-to-use-dynatrace/metrics/metric-ingestion/ingestion-methods/enrich-metrics/).
  If running in a containerized environment with a compatible Dynatrace operator present, additional metadata about the container environment will be added.

```java
MetricBuilderFactory metricBuilderFactory =
    MetricBuilderFactory.builder()
        .withDefaultDimensions(defaultDims)
        .withDynatraceMetadata()
        .withPrefix("prefix")
        .build();
```

This factory can then be used to create new `Metric.Builder` objects, which represent one data point before serialization.
To create metric lines from data points, use a pattern like the following:

```java
metricBuilderFactory
    .newMetricBuilder("my_metric_key")  // the metric key is required.
    .setLongGaugeValue(123)             // metric key and value are the only required fields.
    .setDimensions(dimensions)          // set dynamic dimensions that are specific to the current metric.
    .setCurrentTime()                   // set the current time as timestamp for the data point.
    .serialize()                        // create a String from the information set above.
```

#### Metric line creation options

* `setPrefix`: sets a prefix that will be prepended to the metric key.
* `setDimensions`:
  * When creating the `Metric.Builder` using the `MetricBuilderFactory`: sets the dimensions specific to this metric.
    Default and metadata dimensions will be merged in (see [the section on dimension precedence](#dimension-precedence) below).
  * When using the `Metric.Builder` directly without the factory, either sets a single `DimensionList` on this method, or call `DimensionList.merge` on multiple lists before passing it.
    Merge will be called on the passed list in the serialize method, so if passing a single list it does not have to be de-duplicated.
* `setLongCounterValueDelta` / `setDoubleCounterValueDelta`: sets a single value that is serialized as `count,delta=<value>`.
* `setLongGaugeValue` / `setDoubleGaugeValue`: sets a single value that is serialized as `gauge,<value>`.
* `setLongSummaryValue` / `setDoubleSummaryValue`: sets min, max, sum and count values that are serialized as `gauge,min=<min>,max=<max>,sum=<sum>,count=<count>`.
* `setTimestamp`: sets a specific `Instant` object on the metric that will be used to create the timestamp on the metric line.
* `setCurrentTime`: sets the current timestamp on the `Metric` object.

A metric line can be serialized only if it has a valid key (including the optional prefix) and exactly one `Value` attribute set.
Timestamps and dimensions are optional.

#### Dimension precedence

Since there are multiple levels of dimensions (default, dynamic, Dynatrace metadata) and duplicate keys are not allowed, there is a specified precedence in dimension keys.
Default dimensions will be overwritten by dynamic dimensions, and all dimensions will be overwritten by Dynatrace metadata dimensions if they share the same key after normalization.
Note that the Dynatrace metadata dimensions will only contain [dimension keys reserved by Dynatrace](https://www.dynatrace.com/support/help/how-to-use-dynatrace/metrics/metric-ingestion/metric-ingestion-protocol/#syntax).
If the `.withDynatraceMetadata()` method is not called on the `MetricBuilderFactory`, Dynatrace metadata will not be queried and added.

### Common constants

The library also provides constants that might be helpful in the projects consuming this library.

To access the constants, call the respective static methods on `DynatraceMetricApiConstants`:

```java
String oneAgentEndpoint = DynatraceMetricApiConstants.getDefaultOneAgentEndpoint();
```

Currently available constants are:

* the default [local OneAgent metric API](https://www.dynatrace.com/support/help/how-to-use-dynatrace/metrics/metric-ingestion/ingestion-methods/local-api/) endpoint (`getDefaultOneAgentEndpoint()`)
* the limit for how many metric lines can be ingested in one request (`getPayloadLinesLimit()`)
