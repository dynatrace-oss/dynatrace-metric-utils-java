package com.dynatrace.file.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dynatrace.metric.util.DynatraceMetricApiConstants;
import com.dynatrace.testutils.TestUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DynatraceFileBasedConfigurationProviderTest {
  // We rely on the fact that JUnit runs tests in series, not parallel.

  @Test
  void testNonExistentFileReturnsDefaults() {
    final DynatraceFileBasedConfigurationProvider instance =
        DynatraceFileBasedConfigurationProvider.getInstance();
    // Set up test
    instance.forceOverwriteConfig(TestUtils.generateNonExistentFilename());

    assertEquals(
        DynatraceMetricApiConstants.getDefaultOneAgentEndpoint(),
        instance.getMetricIngestEndpoint());
    assertEquals("", instance.getMetricIngestToken());
  }

  @Test
  void testFileExistsButDoesNotContainRequiredProps_shouldReturnDefault() {
    final DynatraceFileBasedConfigurationProvider instance =
        DynatraceFileBasedConfigurationProvider.getInstance();
    // Set up test
    instance.forceOverwriteConfig("src/test/resources/config_invalid.properties");

    assertEquals(
        DynatraceMetricApiConstants.getDefaultOneAgentEndpoint(),
        instance.getMetricIngestEndpoint());
    assertEquals("", instance.getMetricIngestToken());
  }

  @Test
  void testFileExistsAndContainsValidProps() {
    final DynatraceFileBasedConfigurationProvider instance =
        DynatraceFileBasedConfigurationProvider.getInstance();
    // Set up test
    instance.forceOverwriteConfig("src/test/resources/config_valid.properties");

    assertEquals(
        "https://your-dynatrace-ingest-url/api/v2/metrics/ingest",
        instance.getMetricIngestEndpoint());
    assertEquals("YOUR.DYNATRACE.TOKEN", instance.getMetricIngestToken());
  }

  @Test
  void testConfigIsUpdatedIfFileChanges() throws IOException, InterruptedException {
    final Path tempfile = Files.createTempFile("tempfile", ".properties");
    Files.write(
        tempfile,
        ("DT_METRICS_INGEST_URL = original_url\n" + "DT_METRICS_INGEST_API_TOKEN = original_token")
            .getBytes());
    // wait for the nonblocking io to finish writing.
    Thread.sleep(10);

    final DynatraceFileBasedConfigurationProvider instance =
        DynatraceFileBasedConfigurationProvider.getInstance();
    // Set up test
    instance.forceOverwriteConfig(tempfile.toString());

    assertEquals("original_url", instance.getMetricIngestEndpoint());
    assertEquals("original_token", instance.getMetricIngestToken());

    Files.write(
        tempfile,
        ("DT_METRICS_INGEST_URL = new_url\n" + "DT_METRICS_INGEST_API_TOKEN = new_token")
            .getBytes());
    // wait for nonblocking IO
    Thread.sleep(10);

    assertEquals("new_url", instance.getMetricIngestEndpoint());
    assertEquals("new_token", instance.getMetricIngestToken());
  }
}
