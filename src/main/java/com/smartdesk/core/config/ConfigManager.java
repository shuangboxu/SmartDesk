package com.smartdesk.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Handles loading, saving and tracking configuration changes for the application.
 */
public final class ConfigManager {

    private static final String DEFAULT_RESOURCE = "/com/smartdesk/resources/config.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configFile;
    private final List<Consumer<AppConfig>> listeners = new ArrayList<>();

    private AppConfig currentConfig;
    private Instant lastModified;

    public ConfigManager() {
        this(Paths.get(System.getProperty("user.home"), ".smartdesk", "config.json"));
    }

    public ConfigManager(final Path configFile) {
        this.configFile = Objects.requireNonNull(configFile, "configFile");
        ensureConfigFileExists();
        load();
    }

    public AppConfig getConfig() {
        return currentConfig;
    }

    public void registerListener(final Consumer<AppConfig> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public synchronized void reloadIfChanged() {
        try {
            if (Files.exists(configFile)) {
                Instant fileInstant = Files.getLastModifiedTime(configFile).toInstant();
                if (lastModified == null || fileInstant.isAfter(lastModified)) {
                    load();
                    notifyListeners();
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to reload configuration", ex);
        }
    }

    public synchronized void saveConfig(final AppConfig config) {
        Objects.requireNonNull(config, "config");
        try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            gson.toJson(config, writer);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist configuration", ex);
        }
        this.currentConfig = config;
        try {
            this.lastModified = Files.getLastModifiedTime(configFile).toInstant();
        } catch (IOException ex) {
            this.lastModified = Instant.now();
        }
        notifyListeners();
    }

    private void ensureConfigFileExists() {
        try {
            Files.createDirectories(configFile.getParent());
            if (!Files.exists(configFile)) {
                try (InputStream in = ConfigManager.class.getResourceAsStream(DEFAULT_RESOURCE)) {
                    if (in == null) {
                        throw new IllegalStateException("Missing default configuration resource " + DEFAULT_RESOURCE);
                    }
                    Files.copy(in, configFile);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialise configuration file", ex);
        }
    }

    private void load() {
        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            currentConfig = gson.fromJson(reader, AppConfig.class);
            if (currentConfig == null) {
                currentConfig = new AppConfig();
            }
            lastModified = Files.getLastModifiedTime(configFile).toInstant();
        } catch (JsonSyntaxException | IOException ex) {
            currentConfig = loadFromResource();
            saveConfig(currentConfig);
        }
    }

    private AppConfig loadFromResource() {
        try (InputStream in = ConfigManager.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing default configuration resource " + DEFAULT_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                AppConfig config = gson.fromJson(reader, AppConfig.class);
                return config == null ? new AppConfig() : config;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read default configuration", ex);
        }
    }

    private void notifyListeners() {
        for (Consumer<AppConfig> listener : listeners) {
            listener.accept(currentConfig);
        }
    }
}
