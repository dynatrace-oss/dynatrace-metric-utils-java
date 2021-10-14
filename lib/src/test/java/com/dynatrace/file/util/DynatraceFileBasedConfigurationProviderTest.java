package com.dynatrace.file.util;

import com.dynatrace.metric.util.DynatraceMetricApiConstants;
import com.dynatrace.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynatraceFileBasedConfigurationProviderTest {
    @Test
    void testNonExistentFileReturnsDefaults() {
        // this method is only used in testing
        DynatraceFileBasedConfigurationProvider
                .setupSingleton(TestUtils.generateNonExistentFilename());
        final DynatraceFileBasedConfigurationProvider instance =
                DynatraceFileBasedConfigurationProvider.getInstance();

        assertEquals(DynatraceMetricApiConstants.getDefaultOneAgentEndpoint(), instance.getEndpoint());
        assertEquals("", instance.getToken());
    }

    @Test
    void testFileExistsButDoesNotContainRequiredProps_shouldReturnDefault() {
        DynatraceFileBasedConfigurationProvider.setupSingleton("src/test/resources/config_invalid.properties");
        final DynatraceFileBasedConfigurationProvider instance = DynatraceFileBasedConfigurationProvider.getInstance();

        assertEquals(DynatraceMetricApiConstants.getDefaultOneAgentEndpoint(), instance.getEndpoint());
        assertEquals("", instance.getToken());
    }

    @Test
    void testFileExistsAndContainsValidProps() {
        DynatraceFileBasedConfigurationProvider.setupSingleton("src/test/resources/config_valid.properties");
        final DynatraceFileBasedConfigurationProvider instance = DynatraceFileBasedConfigurationProvider.getInstance();

        assertEquals("https://your-dynatrace-ingest-url/api/v2/metrics/ingest", instance.getEndpoint());
        assertEquals("YOUR.DYNATRACE.TOKEN", instance.getToken());
    }

    @Test
    void testConfigIsUpdatedIfFileChanges() throws IOException, InterruptedException {
        final Path tempfile = Files.createTempFile("tempfile", ".properties");
        Files.writeString(tempfile,
                "DT_METRICS_INGEST_URL = original_url\n" +
                        "DT_METRICS_INGEST_API_TOKEN = original_token");
        // wait for the nonblocking io to finish writing.
        Thread.sleep(1);
        DynatraceFileBasedConfigurationProvider.setupSingleton(tempfile.toString());
        final DynatraceFileBasedConfigurationProvider instance = DynatraceFileBasedConfigurationProvider.getInstance();

        assertEquals("original_url", instance.getEndpoint());
        assertEquals("original_token", instance.getToken());

        Files.writeString(tempfile,
                "DT_METRICS_INGEST_URL = new_url\n" +
                        "DT_METRICS_INGEST_API_TOKEN = new_token");
        // wait for nonblocking IO
        Thread.sleep(1);

        assertEquals("new_url", instance.getEndpoint());
        assertEquals("new_token", instance.getToken());
    }
}