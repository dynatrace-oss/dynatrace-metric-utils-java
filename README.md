# dynatrace-metric-utils-java

Java Utility for preparing communication with the [Dynatrace v2 metrics API](https://www.dynatrace.com/support/help/dynatrace-api/environment-api/metric-v2/).

## Usage

Examples for how to use this library can be found in [the example application](example/src/main/java/com/dynatrace/example/App.java).
It shows how to create metrics lines that can be sent to a [Dynatrace metrics ingest endpoint](https://www.dynatrace.com/support/help/dynatrace-api/environment-api/metric-v2/post-ingest-metrics/).

### Metric line creation

The standard workflow consists of creating a `MetricBuilderFactory` and using it to create `MetricBuilder` objects that can be serialized to a `String`.
Upon creation of a `MetricBuilderFactory`, it is possible to set default dimensions, and a prefix that will be added to all metrics created by the factory.
Furthermore, it is possible to enable OneAgent metadata enrichment in this step.
With this setting enabled, the library will connect to the Dynatrace OneAgent, if installed, and retrieve process and host identifiers that are added as dimensions on all metrics to correlate them accordingly.

```java
MetricBuilderFactory metricBuilderFactory =
    MetricBuilderFactory.builder()
        .withDefaultDimensions(defaultDims)
        .withOneAgentMetadata()
        .withPrefix("prefix")
        .build();
```

This factory can then be used to create new `Metric.Builder` objects, which represent one data point before serialization.
To create metric lines from data points, use a pattern like the following:

```java
metricBuilderFactory
    .newMetricBuilder("my_metric_key")  // the metric key is required.
    .setLongCounterValue(123)           // metric key and value are the only required fields.
    .setDimensions(dimensions)          // set dynamic dimensions that are specific to the current metric.
    .setCurrentTime()                   // set the current time as timestamp for the data point.
    .serialize()                        // create a String from the information set above.
```

#### Metric line creation options

* `setPrefix`: sets a prefix that will be prepended to the metric key.
* `setDimensions`:
  * When creating the `Metric.Builder` using the `MetricBuilderFactory`: sets the dimensions specific to this metric.
    Default and OneAgent dimensions will be merged in (see [information on precedence](#dimension-precedence) below).
  * When using the `Metric.Builder` directly without the factory, either sets a single `DimensionList` on this method, or call `DimensionList.merge` on multiple lists before passing it.
    Merge will be called on the passed list in the serialize function, so if passing a single list it does not have to be de-duplicated.
* `setLongCounterValueTotal` / `setDoubleCounterValueTotal`: sets a single value that is serialized as `count,<value>`.
* `setLongCounterValueDelta` / `setDoubleCounterValueDelta`: sets a single value that is serialized as `count,delta=<value>`.
* `setLongGaugeValue` / `setDoubleGaugeValue`: sets a single value that is serialized as `gauge,<value>`.
* `setLongSummaryValue` / `setDoubleSummaryValue`: sets min, max, sum and count values that are serialized as `gauge,min=<min>,max=<max>,sum=<sum>,count=<count>`.
* `setTimestamp`: sets a specific `Instant` object on the metric that will be used to create the timestamp on the metric line.
* `setCurrentTime`: sets the current timestamp on the `Metric` object.

A metric line can be serialized only if it has a valid key (including the optional prefix) and exactly one `Value` attribute set.
Timestamps and dimensions are optional.

#### Dimension precedence

Since there are multiple levels of dimensions (default, dynamic, OneAgent) and duplicate keys are not allowed, there is a specified precedence in dimension keys.
Default dimensions will be overwritten by dynamic dimensions, and all dimensions will be overwritten by OneAgent dimensions if they share the same key after normalization.
Note that the OneAgent dimensions will only contain [dimension keys reserved by Dynatrace](https://www.dynatrace.com/support/help/how-to-use-dynatrace/metrics/metric-ingestion/metric-ingestion-protocol/#syntax).
If the `.withOneAgentMetadata()` method is not called on the `MetricBuilderFactory`, OneAgent metadata will not be queried and added.
