/**
 * Copyright 2022 Dynatrace LLC
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
package com.dynatrace.file.util;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dynatrace.metric.util.DynatraceMetricApiConstants;
import com.dynatrace.testutils.TestUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

    instance.forceOverwriteConfig(null, null);
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

    instance.forceOverwriteConfig(null, null);
  }

  @Test
  void testFileExistsAndContainsValidProps() {
    final DynatraceFileBasedConfigurationProvider instance =
        DynatraceFileBasedConfigurationProvider.getInstance();
    // Set up test
    instance.forceOverwriteConfig(
        "src/test/resources/config_valid.properties", Duration.ofMillis(50));

    await()
        .atMost(500, TimeUnit.MILLISECONDS)
        .until(
            () ->
                "https://your-dynatrace-ingest-url/api/v2/metrics/ingest"
                    .equals(instance.getMetricIngestEndpoint()));

    assertEquals(
        "https://your-dynatrace-ingest-url/api/v2/metrics/ingest",
        instance.getMetricIngestEndpoint());
    assertEquals("YOUR.DYNATRACE.TOKEN", instance.getMetricIngestToken());

    instance.forceOverwriteConfig(null, null);
  }

  @Test
  void testConfigIsUpdatedIfFileChanges() throws IOException {
    final Path tempfile = Files.createTempFile(tempDir, "tempfile", ".properties");
    Files.write(
        tempfile,
        ("DT_METRICS_INGEST_URL = original_url\n" + "DT_METRICS_INGEST_API_TOKEN = original_token")
            .getBytes());

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
        .atMost(500, TimeUnit.MILLISECONDS)
        .until(() -> instance.getMetricIngestEndpoint().equals("new_url"));
    assertEquals("new_url", instance.getMetricIngestEndpoint());
    assertEquals("new_token", instance.getMetricIngestToken());

    Files.delete(tempfile);
    instance.forceOverwriteConfig(null, null);
  }
}
