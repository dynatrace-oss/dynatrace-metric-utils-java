package com.dynatrace.file.util;

import com.dynatrace.metric.util.DynatraceMetricApiConstants;
import com.dynatrace.testutils.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DynatraceFileBasedConfigurationProviderTest {
  // We rely on the fact that JUnit runs tests in series, not parallel.

  static Path tempDir;

  @BeforeAll
  static void setUpClass() throws IOException {
    tempDir = Files.createTempDirectory("tempdir");
  }

  @AfterAll
  static void tearDown() throws IOException {
    Files.deleteIfExists(tempDir);
  }

  @Test
  void testNonExistentFileReturnsDefaults() {
    final DynatraceFileBasedConfigurationProvider instance =
        DynatraceFileBasedConfigurationProvider.getInstance();
    // Set up test
    instance.forceOverwriteConfig(TestUtils.generateNonExistentFilename(), Duration.ofMillis(500));

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
    instance.forceOverwriteConfig(
        "src/test/resources/config_invalid.properties", Duration.ofMillis(500));

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
    instance.forceOverwriteConfig(
        "src/test/resources/config_valid.properties", Duration.ofMillis(50));

    await()
        .atMost(150, TimeUnit.MILLISECONDS)
        .until(
            () ->
                "https://your-dynatrace-ingest-url/api/v2/metrics/ingest"
                    .equals(instance.getMetricIngestEndpoint()));

    assertEquals(
        "https://your-dynatrace-ingest-url/api/v2/metrics/ingest",
        instance.getMetricIngestEndpoint());
    assertEquals("YOUR.DYNATRACE.TOKEN", instance.getMetricIngestToken());
  }

  @Test
  void testConfigIsUpdatedIfFileChanges() throws IOException, InterruptedException {
    final Path tempfile = Files.createTempFile(tempDir, "tempfile", ".properties");
    Files.write(
        tempfile,
        ("DT_METRICS_INGEST_URL = original_url\n" + "DT_METRICS_INGEST_API_TOKEN = original_token")
            .getBytes());
    // wait for the nonblocking io to finish writing.
    Thread.sleep(30);

    final DynatraceFileBasedConfigurationProvider instance =
        DynatraceFileBasedConfigurationProvider.getInstance();
    // Set up test
    instance.forceOverwriteConfig(tempfile.toString(), Duration.ofMillis(50));

    assertEquals("original_url", instance.getMetricIngestEndpoint());
    assertEquals("original_token", instance.getMetricIngestToken());

    Files.write(
        tempfile,
        ("DT_METRICS_INGEST_URL = new_url\n" + "DT_METRICS_INGEST_API_TOKEN = new_token")
            .getBytes());

    // wait for nonblocking IO
    await()
        .atMost(150, TimeUnit.MILLISECONDS)
        .until(() -> instance.getMetricIngestEndpoint().equals("new_url"));
    assertEquals("new_url", instance.getMetricIngestEndpoint());
    assertEquals("new_token", instance.getMetricIngestToken());
    Files.delete(tempfile);
  }
}
