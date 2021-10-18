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
    config = new DynatraceConfiguration();
    FilePoller poller = null;
    try {
      if (!Files.exists(Paths.get(fileName))) {
        logger.info("File based configuration does not exist, serving default config.");
      } else {
        poller = new FilePoller(fileName);
      }
    } catch (InvalidPathException e) {
      logger.warning(String.format("%s is not a valid file path (%s).", fileName, e.getMessage()));
    } catch (IOException | IllegalArgumentException e) {
      logger.warning(String.format("WatchService could not be initialized: %s", e.getMessage()));
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
      logger.finest("Config is up to date.");
      return;
    }
    // read the properties from the file
    try (FileInputStream inputStream = new FileInputStream(fileName)) {
      Properties props = new Properties();
      props.load(inputStream);

      final String newEndpoint = tryGetEndpoint(props);
      if (newEndpoint != null) {
        config.setEndpoint(newEndpoint);
      }

      final String newToken = tryGetToken(props);
      if (newToken != null) {
        config.setToken(newToken);
      }

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
    } else {
      if (!newToken.equals(config.getToken())) {
        logger.info("API Token refreshed.");
        return newToken;
      }
    }
    return null;
  }

  /**
   * Tries to get the Endpoint from the {@link Properties properties} object.
   *
   * @return The new endpoint if it is available and different to the previous one, and null
   *     otherwise.
   */
  private String tryGetEndpoint(Properties props) {
    final String newEndpoint = props.getProperty("DT_METRICS_INGEST_URL");
    if (newEndpoint == null) {
      logger.fine("Could not read property with key 'DT_METRICS_INGEST_URL'.");
    } else {
      if (!newEndpoint.equals(config.getEndpoint())) {
        logger.info(String.format("Read new endpoint: %s", newEndpoint));
        return newEndpoint;
      }
    }
    return null;
  }

  private void updateConfigIfChanged() {
    if (filePoller != null && filePoller.fileContentsUpdated()) {
      updateConfigFromFile(filePoller.getWatchedFilePath());
    }
  }

  public String getEndpoint() {
    updateConfigIfChanged();
    return config.getEndpoint();
  }

  public String getToken() {
    updateConfigIfChanged();
    return config.getToken();
  }
}
