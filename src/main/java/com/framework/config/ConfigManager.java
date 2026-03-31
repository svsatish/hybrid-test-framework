package com.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton ConfigManager that loads environment-specific configuration.
 * <p>
 * Loading order (later wins):
 * <ol>
 *   <li>{@code config.properties} — base defaults</li>
 *   <li>{@code config-{env}.properties} — environment overlay (set via {@code -Denv=qa|staging|prod})</li>
 *   <li>System properties ({@code -Dkey=value})</li>
 *   <li>OS environment variables ({@code KEY_NAME=value})</li>
 * </ol>
 * <p>
 * Usage: {@code ./gradlew test -Denv=staging}
 */
public class ConfigManager {

    private static final Logger LOG = LogManager.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    private final Properties properties;
    private final String activeEnv;

    private ConfigManager() {
        properties = new Properties();

        // 1. Load base config.properties
        loadPropertiesFile("config.properties");

        // 2. Determine active environment from -Denv or ENV env var
        activeEnv = resolveActiveEnv();
        LOG.info("Active environment: {}", activeEnv);

        // 3. Overlay environment-specific config if it exists
        if (activeEnv != null && !activeEnv.isEmpty()) {
            String envFile = "config-" + activeEnv + ".properties";
            loadPropertiesFile(envFile);
        }
    }

    private void loadPropertiesFile(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                properties.load(is);
                LOG.info("Loaded {} successfully", filename);
            } else {
                LOG.warn("{} not found on classpath", filename);
            }
        } catch (IOException e) {
            LOG.error("Failed to load {}", filename, e);
            throw new RuntimeException("Could not load " + filename, e);
        }
    }

    private String resolveActiveEnv() {
        // -Denv=staging takes highest priority
        String env = System.getProperty("env");
        if (env != null && !env.isBlank()) return env.trim().toLowerCase();

        // Fallback to ENV environment variable
        String envVar = System.getenv("ENV");
        if (envVar != null && !envVar.isBlank()) return envVar.trim().toLowerCase();

        return null; // no environment override — use base config only
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Returns the active environment name (qa, staging, prod) or "default" if none set.
     */
    public String getActiveEnvironment() {
        return activeEnv != null ? activeEnv : "default";
    }

    /**
     * Returns config value with precedence:
     * System property &gt; Environment variable &gt; config-{env}.properties &gt; config.properties
     */
    public String get(String key) {
        String sysProp = System.getProperty(key);
        if (sysProp != null) return sysProp;

        String envVar = System.getenv(key.replace('.', '_').toUpperCase());
        if (envVar != null) return envVar;

        return properties.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            LOG.warn("Invalid integer for key '{}': '{}'. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    // Convenience accessors
    public String getApiBaseUrl() {
        return get("api.base.url");
    }

    public String getUiBaseUrl() {
        return get("ui.base.url");
    }

    public String getBrowser() {
        return get("browser", "chrome");
    }

    public boolean isHeadless() {
        return getBoolean("headless", false);
    }

    public int getTimeout() {
        return getInt("timeout.seconds", 10);
    }

    public String getUiUsername() {
        return get("ui.username");
    }

    public String getUiPassword() {
        return get("ui.password");
    }

    public boolean isVideoEnabled() {
        return getBoolean("video.enabled", false);
    }
}

