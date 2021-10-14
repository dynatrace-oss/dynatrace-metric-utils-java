package com.dynatrace.file.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class DynatraceFileBasedConfigurationProvider {
    private static DynatraceFileBasedConfigurationProvider singletonInstance = null;

    private static final Logger logger =
            Logger.getLogger(DynatraceFileBasedConfigurationProvider.class.getName());

    private static final String PROPERTIES_FILENAME =
            "/var/lib/dynatrace/enrichment/endpoint/endpoint.properties";

    private final FilePoller filePoller;
    private final DynatraceConfiguration config;

    // VisibleForTesting
    static void setupSingleton(String fileName) {
        singletonInstance = new DynatraceFileBasedConfigurationProvider(fileName);
    }

    public static DynatraceFileBasedConfigurationProvider getInstance() {
        if (singletonInstance == null) {
            setupSingleton(PROPERTIES_FILENAME);
        }
        return singletonInstance;
    }

    public DynatraceFileBasedConfigurationProvider(String fileName) {
        config = new DynatraceConfiguration();
        FilePoller poller = null;
        if (!Files.exists(Paths.get(fileName))) {
            logger.warning("File based configuration does not exist, serving default config.");
        } else {
            try {
                poller = new FilePoller(fileName);
            } catch (IOException | IllegalArgumentException e) {
                logger.warning(String.format("WatchService could not be initialized: %s",
                        e.getMessage()));
            }
        }
        filePoller = poller;
        // try to read from file
        updateConfigFromFile(fileName);
    }

    private void updateConfigFromFile(String fileName) {
        if (filePoller == null) {
            // nothing to do, as no watch service is set up.
            return;
        }
        // read the properties from the file
        try (FileInputStream inputStream = new FileInputStream(fileName)) {
            Properties props = new Properties();
            props.load(inputStream);
            final String newEndpoint = props.getProperty("DT_METRICS_INGEST_URL");
            if (newEndpoint == null) {
                logger.fine("Could not read property with key 'DT_METRICS_INGEST_URL'.");
            } else {
                if (!newEndpoint.equals(config.getEndpoint())) {
                    logger.info(String.format("Read new endpoint: %s", newEndpoint));
                    config.setEndpoint(newEndpoint);
                }
            }
            final String newToken = props.getProperty("DT_METRICS_INGEST_API_TOKEN");
            if (newToken == null) {
                logger.warning("Could not read property with key 'DT_METRICS_INGEST_API_TOKEN'.");
            } else {
                if (!newToken.equals(config.getToken())) {
                    logger.info("API Token refreshed.");
                    config.setToken(newToken);
                }
            }
        } catch (IOException e) {
            logger.info("Failed reading properties from file.");
        }
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

