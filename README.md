# dynatrace-metric-utils-java

Java Utility for preparing communication with the [Dynatrace v2 metrics API](https://www.dynatrace.com/support/help/dynatrace-api/environment-api/metric-v2/).

## Usage

Examples for how to use this library can be found in [the example application](example/src/main/java/com/dynatrace/example/App.java).
It shows how to create metrics lines that can be sent to any [Dynatrace metrics ingest endpoint](https://www.dynatrace.com/support/help/dynatrace-api/environment-api/metric-v2/post-ingest-metrics/)

### Metric line creation

The standard workflow consists of creating a `MetricBuilderFactory` and using it to create `MetricBuilder` objects that can be serialized to a `String`.
Upon creation of a `MetricBuilderFactory` it is possible to set default dimensions and a prefix that will be added to all metrics created by the factory.
Furthermore, it is possible to enable OneAgent metadata enrichment in this step:

```java
MetricBuilderFactory metricBuilderFactory =
    MetricBuilderFactory.builder()
        .withDefaultDimensions(defaultDims)
        .withOneAgentMetadata()
        .withPrefix("prefix")
        .build();
```

This factory can then be used to create new `Metric.Builder` objects, which represent one metric line before serialization.
To create new metric lines, use a pattern like the following:

```java
metricBuilderFactory
    .newMetricBuilder("metric1")    // the metric name is required.
    .setIntCounterValue(123)        // name and value are the only required fields.
    .setDimensions(dimensions)      // set dynamic dimensions that are specific to the current metric.
    .setCurrentTime()               // if you want a timestamp on your metric line.
    .serialize()                    // create a String from the information set above.
```

#### Dimension precedence

Since there are multiple levels of dimensions (default, dynamic, OneAgent) and duplicate keys are not allowed, there is a specified precedence in dimension keys.
Default dimensions will be overwritten by dynamic dimensions, and all dimensions will be overwritten by OneAgent dimensions if they share the same key after normalization.
If the `.withOneAgentMetadata()` function is not called on the `MetricBuilderFactory`, OneAgent data will not be exported.
