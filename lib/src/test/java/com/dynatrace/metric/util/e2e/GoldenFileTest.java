/**
 * Copyright 2026 Dynatrace LLC
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynatrace.metric.util.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dynatrace.metric.util.MetricException;
import com.dynatrace.metric.util.MetricLineBuilder;
import com.dynatrace.metric.util.MetricLinePreConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Builds metric and metadata lines through the public API only and compares them with the expected
 * output in {@code src/test/resources/golden/metric-lines.txt}. This pins the serialized format
 * that the Dynatrace ingest endpoint receives.
 *
 * <p>After an intentional change of the output, regenerate the file with {@code ./gradlew :lib:test
 * --tests '*GoldenFileTest' -PupdateGolden} and review the diff.
 */
class GoldenFileTest {
  private static final Path GOLDEN_FILE = Paths.get("src/test/resources/golden/metric-lines.txt");
  // the OneAgent writes this file to the working directory of monitored processes
  private static final Path INDIRECTION_FILE =
      Paths.get("dt_metadata_e617c525669e072eebe3d0f08212e8f2.properties");
  private static final Instant TIMESTAMP = Instant.ofEpochMilli(1_700_000_000_000L);

  @FunctionalInterface
  private interface Case {
    String build() throws MetricException;
  }

  /** Provide Dynatrace metadata the way the OneAgent does, for cases that enable it. */
  @BeforeEach
  void writeDynatraceMetadata(@TempDir Path tempDir) throws IOException {
    Path metadataFile = tempDir.resolve("dt_metadata.properties");
    write(metadataFile, "dt.entity.process_group_instance=PROCESS_GROUP_INSTANCE-1234\n");
    write(INDIRECTION_FILE, metadataFile.toAbsolutePath().toString());
  }

  @AfterEach
  void deleteDynatraceMetadata() throws IOException {
    Files.delete(INDIRECTION_FILE);
  }

  @Test
  void matchesGoldenFile() throws IOException {
    StringBuilder actual = new StringBuilder();
    cases()
        .forEach(
            (name, c) ->
                actual.append("=== ").append(name).append('\n').append(run(c)).append('\n'));

    if (Boolean.getBoolean("updateGolden")) {
      write(GOLDEN_FILE, actual.toString());
    } else {
      assertEquals(
          new String(Files.readAllBytes(GOLDEN_FILE), StandardCharsets.UTF_8), actual.toString());
    }
  }

  private static Map<String, Case> cases() {
    Map<String, Case> cases = new LinkedHashMap<>();

    // ===== payload types and values =====
    cases.put("gauge value", () -> metric("my.gauge").gauge().value(1.5).build());
    cases.put("gauge integer value", () -> metric("my.gauge").gauge().value(200).build());
    cases.put("gauge negative value", () -> metric("my.gauge").gauge().value(-0.25).build());
    cases.put("gauge zero", () -> metric("my.gauge").gauge().value(-0.0).build());
    cases.put("gauge large value", () -> metric("my.gauge").gauge().value(1e10).build());
    cases.put(
        "gauge large fraction",
        () -> metric("my.gauge").gauge().value(1_234_567_000_000.123).build());
    cases.put("gauge max double", () -> metric("my.gauge").gauge().value(Double.MAX_VALUE).build());
    cases.put("gauge min double", () -> metric("my.gauge").gauge().value(Double.MIN_VALUE).build());
    cases.put(
        "gauge precision", () -> metric("my.gauge").gauge().value(1.1234567890123456789).build());
    cases.put("gauge summary", () -> metric("my.gauge").gauge().summary(0.1, 10, 20.5, 7).build());
    cases.put("gauge empty summary", () -> metric("my.gauge").gauge().summary(0, 0, 0, 0).build());
    cases.put("counter delta", () -> metric("my.count").count().delta(42).build());
    cases.put("counter fractional delta", () -> metric("my.count").count().delta(0.123).build());
    cases.put("counter negative delta", () -> metric("my.count").count().delta(-3).build());

    // ===== timestamps =====
    cases.put("timestamp", () -> metric("my.gauge").gauge().value(1).timestamp(TIMESTAMP).build());
    cases.put(
        "timestamp with nanoseconds",
        () ->
            metric("my.gauge")
                .gauge()
                .value(1)
                .timestamp(Instant.ofEpochSecond(1_700_000_000L, 123_456_789L))
                .build());
    cases.put(
        "timestamp in the future",
        () ->
            metric("my.gauge")
                .gauge()
                .value(1)
                .timestamp(Instant.ofEpochSecond(4_102_444_800L))
                .build());
    cases.put(
        "timestamp before 2000 is dropped",
        () -> metric("my.gauge").gauge().value(1).timestamp(Instant.ofEpochSecond(0)).build());
    cases.put(
        "null timestamp is dropped",
        () -> metric("my.gauge").gauge().value(1).timestamp(null).build());

    // ===== invalid values =====
    cases.put("gauge NaN", () -> metric("my.gauge").gauge().value(Double.NaN).build());
    cases.put(
        "gauge infinity", () -> metric("my.gauge").gauge().value(Double.POSITIVE_INFINITY).build());
    cases.put("counter NaN", () -> metric("my.count").count().delta(Double.NaN).build());
    cases.put(
        "counter negative infinity",
        () -> metric("my.count").count().delta(Double.NEGATIVE_INFINITY).build());
    cases.put(
        "summary negative count", () -> metric("my.gauge").gauge().summary(0, 0, 0, -1).build());
    cases.put(
        "summary zero count with values",
        () -> metric("my.gauge").gauge().summary(1, 2, 3, 0).build());
    cases.put(
        "summary max below min", () -> metric("my.gauge").gauge().summary(5, 3, 8, 2).build());
    cases.put(
        "summary average outside min and max",
        () -> metric("my.gauge").gauge().summary(1, 2, 10, 2).build());
    cases.put("summary NaN", () -> metric("my.gauge").gauge().summary(Double.NaN, 1, 1, 1).build());
    cases.put(
        "summary infinity",
        () -> metric("my.gauge").gauge().summary(0, Double.POSITIVE_INFINITY, 1, 1).build());

    // ===== metric keys =====
    cases.put(
        "key with invalid characters", () -> metric("my!metric@key").gauge().value(1).build());
    cases.put("key with leading digit", () -> metric("1metric").gauge().value(1).build());
    cases.put("key with uppercase", () -> metric("My.Metric").gauge().value(1).build());
    cases.put("key with empty section", () -> metric("my..metric").gauge().value(1).build());
    cases.put("key truncated", () -> metric(repeat("a", 300)).gauge().value(1).build());
    cases.put("key at max length", () -> metric(repeat("a", 255)).gauge().value(1).build());
    cases.put("key one over max length", () -> metric(repeat("a", 256)).gauge().value(1).build());
    cases.put("key with lone surrogate", () -> metric("my\uD800key").gauge().value(1).build());
    cases.put("empty key", () -> metric("").gauge().value(1).build());
    cases.put("null key", () -> metric((String) null).gauge().value(1).build());
    cases.put("key invalid after normalization", () -> metric("!@#").gauge().value(1).build());

    // ===== dimensions =====
    cases.put(
        "dimensions",
        () ->
            metric("my.metric")
                .dimension("dim1", "value1")
                .dimension("dim2", "value2")
                .gauge()
                .value(1)
                .build());
    cases.put("dimension key normalization", () -> withDimension("Dim!Key-With_Stuff", "value"));
    cases.put("dimension key with leading digit", () -> withDimension("1dim", "value"));
    cases.put("dimension key truncated", () -> withDimension(repeat("k", 150), "value"));
    cases.put("empty dimension key is dropped", () -> withDimension("", "value"));
    cases.put("empty dimension value is dropped", () -> withDimension("dim", ""));
    cases.put(
        "dimension value escaping",
        () -> withDimension("dim", "a value, with \"quotes\", = and \\ backslash"));
    cases.put("dimension value with emoji", () -> withDimension("dim", "😊"));
    cases.put(
        "dimension value with control characters",
        () -> withDimension("dim", "line\nbreak\ttab\u0000null"));
    cases.put("dimension value with unicode", () -> withDimension("dim", "äöüß"));
    cases.put("dimension key at max length", () -> withDimension(repeat("k", 100), "value"));
    cases.put("dimension key one over max length", () -> withDimension(repeat("k", 101), "value"));
    cases.put("dimension value at max length", () -> withDimension("dim", repeat("v", 255)));
    cases.put("dimension value one over max length", () -> withDimension("dim", repeat("v", 256)));
    cases.put(
        "dimension value truncated inside surrogate pair",
        () -> withDimension("dim", repeat("v", 254) + "😊"));
    cases.put("dimension value with lone surrogate", () -> withDimension("dim", "a\uD800b"));
    cases.put("dimension value truncated", () -> withDimension("dim", repeat("v", 300)));
    cases.put(
        "dimension value truncated before escape",
        () -> withDimension("dim", repeat("v", 254) + "\""));
    cases.put(
        "same dimension key twice",
        () ->
            metric("my.metric")
                .dimension("dim", "first")
                .dimension("dim", "second")
                .gauge()
                .value(1)
                .build());
    cases.put(
        "dimensions at max count",
        // the order of dimensions is not specified, so only count them
        () -> {
          String line = metric("my.metric").dimensions(dims(50)).gauge().value(1).build();
          return (line.split(" ")[0].split(",").length - 1) + " dimensions";
        });
    cases.put(
        "too many dimensions",
        () -> metric("my.metric").dimensions(dims(51)).gauge().value(1).build());

    // ===== pre-configuration =====
    cases.put("prefix", () -> metric(preConfig().prefix("prefix")).gauge().value(1).build());
    cases.put("invalid prefix", () -> metric(preConfig().prefix("!@#")).gauge().value(1).build());
    cases.put(
        "default dimensions",
        () ->
            metric(preConfig().defaultDimensions(dims("default1", "value1", "default2", "value2")))
                .dimension("dim", "value")
                .gauge()
                .value(1)
                .build());
    cases.put(
        "dimension overrides default dimension",
        () ->
            metric(preConfig().defaultDimensions(dims("dim", "default", "other", "default")))
                .dimension("dim", "dynamic")
                .gauge()
                .value(1)
                .build());
    cases.put(
        "default dimension key normalization",
        () ->
            metric(preConfig().defaultDimensions(dims("Default!Dim", "value")))
                .gauge()
                .value(1)
                .build());
    cases.put(
        "too many default dimensions",
        () -> metric(preConfig().defaultDimensions(dims(51))).gauge().value(1).build());
    cases.put(
        "default and dynamic dimensions above limit",
        () ->
            metric(preConfig().defaultDimensions(dims(50)))
                .dimension("one.too.many", "value")
                .gauge()
                .value(1)
                .build());
    cases.put(
        "dynatrace metadata",
        () ->
            metric(
                    preConfig()
                        .prefix("prefix")
                        .defaultDimensions(dims("default", "value"))
                        .dynatraceMetadataDimensions())
                .dimension("dim", "value")
                .count()
                .delta(1)
                .timestamp(TIMESTAMP)
                .build());
    cases.put(
        "dynatrace metadata overrides dimensions",
        () ->
            metric(
                    preConfig()
                        .defaultDimensions(dims("dt.entity.process_group_instance", "default"))
                        .dynatraceMetadataDimensions())
                .dimension("dt.entity.process_group_instance", "dynamic")
                .gauge()
                .value(1)
                .build());

    // ===== metadata lines =====
    cases.put(
        "gauge metadata",
        () -> {
          MetricLineBuilder.GaugeStep gauge = metric("my.gauge").gauge();
          return gauge.value(1).build()
              + "\n"
              + gauge
                  .metadata()
                  .description("A description of the metric")
                  .unit("unit")
                  .displayName("My Gauge")
                  .build();
        });
    cases.put(
        "counter metadata",
        () -> {
          MetricLineBuilder.CounterStep counter = metric("my.count").count();
          return counter.delta(1).build()
              + "\n"
              + counter.metadata().description("Measures something in bytes").unit("Byte").build();
        });
    cases.put(
        "metadata escaping",
        () ->
            metric("my.gauge")
                .gauge()
                .metadata()
                .description("with \"quotes\", commas, = and \\ backslash")
                .displayName("display, \"name\"")
                .build());
    // backslashes are not escaped in quoted metadata strings, so these produce unbalanced quotes
    cases.put(
        "metadata with trailing backslash",
        () -> metric("my.gauge").gauge().metadata().description("C:\\temp\\").build());
    cases.put(
        "metadata with escaped quote",
        () -> metric("my.gauge").gauge().metadata().description("say \\\"hi\\\"").build());
    cases.put(
        "metadata with invalid unit",
        () -> metric("my.gauge").gauge().metadata().description("valid").unit("bad unit").build());
    cases.put("metadata without fields", () -> metric("my.gauge").gauge().metadata().build());
    cases.put(
        "metadata with prefix and dimensions",
        () -> {
          MetricLineBuilder.GaugeStep gauge =
              metric(preConfig().prefix("prefix")).dimension("dim", "value").gauge();
          return gauge.value(1).build()
              + "\n"
              + gauge.metadata().description("dimensions are not added").build();
        });

    return cases;
  }

  private static String run(Case c) {
    try {
      return c.build();
    } catch (MetricException e) {
      return "ERROR: " + e.getMessage();
    }
  }

  private static MetricLineBuilder.TypeStep metric(String key) throws MetricException {
    return MetricLineBuilder.create().metricKey(key);
  }

  private static MetricLineBuilder.TypeStep metric(MetricLinePreConfiguration.Builder preConfig)
      throws MetricException {
    return MetricLineBuilder.create(preConfig.build()).metricKey("my.metric");
  }

  private static MetricLinePreConfiguration.Builder preConfig() {
    return MetricLinePreConfiguration.builder();
  }

  private static String withDimension(String key, String value) throws MetricException {
    return metric("my.metric").dimension(key, value).gauge().value(1).build();
  }

  /** Dimensions in insertion order, from alternating keys and values. */
  private static Map<String, String> dims(String... keysAndValues) {
    Map<String, String> dims = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      dims.put(keysAndValues[i], keysAndValues[i + 1]);
    }
    return dims;
  }

  private static Map<String, String> dims(int count) {
    Map<String, String> dims = new LinkedHashMap<>();
    for (int i = 0; i < count; i++) {
      dims.put("dim" + i, "value" + i);
    }
    return dims;
  }

  private static String repeat(String s, int times) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < times; i++) {
      sb.append(s);
    }
    return sb.toString();
  }

  private static void write(Path file, String content) throws IOException {
    Files.write(file, content.getBytes(StandardCharsets.UTF_8));
  }
}
