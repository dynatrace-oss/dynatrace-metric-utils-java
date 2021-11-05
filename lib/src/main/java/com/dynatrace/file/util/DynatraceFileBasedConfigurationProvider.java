package com.dynatrace.file.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class DynatraceFileBasedConfigurationProvider {
  // Lazy loading singleton instance.
  private static class ProviderHolder {
    private static final DynatraceFileBasedConfigurationProvider INSTANCE =
        new DynatraceFileBasedConfigurationProvider(PROPERTIES_FILENAME);
  }

  // this is used so on the initial read no logs will be printed
  private boolean alreadyInitialized = false;

  private DynatraceFileBasedConfigurationProvider(String fileName) {
    setUp(fileName);
  }

  private static final Logger logger =
      Logger.getLogger(DynatraceFileBasedConfigurationProvider.class.getName());

  private static final String PROPERTIES_FILENAME =
      "/var/lib/dynatrace/enrichment/endpoint/endpoint.properties";

  private FilePoller filePoller;
  private DynatraceConfiguration config;

  public static DynatraceFileBasedConfigurationProvider getInstance() {
    return ProviderHolder.INSTANCE;
  }

  private void setUp(String fileName) {
    alreadyInitialized = false;
    config = new DynatraceConfiguration();
    FilePoller poller = null;
    try {
      if (!Files.exists(Paths.get(fileName))) {
        logger.info("File based configuration does not exist, serving default config.");
      } else {
        poller = new FilePoller(fileName);
      }
    } catch (InvalidPathException e) {
      // This happens on windows, when the linux filepath is not valid.
      logger.info(
          () -> String.format("%s is not a valid file path (%s).", fileName, e.getMessage()));
    } catch (IOException | IllegalArgumentException e) {
      logger.warning(
          () -> String.format("WatchService could not be initialized: %s", e.getMessage()));
    }
    filePoller = poller;
    // try to read from file
    updateConfigFromFile(fileName);
  }

  // This method should never be called by user code. It is only available for testing.
  // VisibleForTesting
  void forceOverwriteConfig(String fileName) {
    logger.warning("Overwriting config. This should ONLY happen in testing.");
    setUp(fileName);
  }

  private void updateConfigFromFile(String fileName) {
    if (filePoller == null) {
      // nothing to do, as no watch service is set up.
      logger.finest("No file watch set up, serving default values.");
      return;
    }
    // read the properties from the file
    try (FileInputStream inputStream = new FileInputStream(fileName)) {
      Properties props = new Properties();
      props.load(inputStream);

      final String newEndpoint = tryGetMetricsEndpoint(props);
      if (newEndpoint != null) {
        config.setMetricIngestEndpoint(newEndpoint);
      }

      final String newToken = tryGetToken(props);
      if (newToken != null) {
        config.setMetricIngestToken(newToken);
      }

      alreadyInitialized = true;
    } catch (IOException e) {
      logger.info("Failed reading properties from file.");
    }
  }

  /**
   * Tries to get the token from the {@link Properties properties} object.
   *
   * @return the new token, if it is available and different to the previous one, or null otherwise.
   */
  private String tryGetToken(Properties props) {
    final String newToken = props.getProperty("DT_METRICS_INGEST_API_TOKEN");
    if (newToken == null) {
      logger.warning("Could not read property with key 'DT_METRICS_INGEST_API_TOKEN'.");
      return null;
    }
    if (!newToken.equals(config.getMetricIngestToken())) {
      if (alreadyInitialized) {
        logger.info("API Token refreshed.");
      }
      return newToken;
    }
    return null;
  }

  /**
   * Tries to get the Endpoint from the {@link Properties properties} object.
   *
   * @return The new endpoint if it is available and different to the previous one, and null
   *     otherwise.
   */
  private String tryGetMetricsEndpoint(Properties props) {
    final String newEndpoint = props.getProperty("DT_METRICS_INGEST_URL");
    if (newEndpoint == null) {
      logger.fine("Could not read property with key 'DT_METRICS_INGEST_URL'.");
      return null;
    }
    if (!newEndpoint.equals(config.getMetricIngestEndpoint())) {
      if (alreadyInitialized) {
        logger.info(() -> String.format("Read new endpoint: %s", newEndpoint));
      }
      return newEndpoint;
    }
    return null;
  }

  private void updateConfigIfChanged() {
    if (filePoller != null && filePoller.fileContentsUpdated()) {
      updateConfigFromFile(filePoller.getWatchedFilePath());
    }
  }

  public String getMetricIngestEndpoint() {
    updateConfigIfChanged();
    return config.getMetricIngestEndpoint();
  }

  public String getMetricIngestToken() {
    updateConfigIfChanged();
    return config.getMetricIngestToken();
  }
}
