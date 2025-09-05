package com.stockcharts.app.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Config {

    private static String polygonApiKey;

    public static String getPolygonApiKey() {
        if (polygonApiKey != null) return polygonApiKey;

        // 1) Environment variable override
        String fromEnv = System.getenv("POLYGON_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            polygonApiKey = fromEnv.trim();
            return polygonApiKey;
        }

        // 2) Local properties file at project root: config/local.properties
        Path path = Paths.get("config", "local.properties");
        if (Files.exists(path)) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                props.load(fis);
                String key = props.getProperty("POLYGON_API_KEY");
                if (key != null && !key.isBlank()) {
                    polygonApiKey = key.trim();
                    return polygonApiKey;
                }
            } catch (IOException ignored) {}
        }

        throw new IllegalStateException("Missing Polygon API key. Set env POLYGON_API_KEY or create config/local.properties with POLYGON_API_KEY=<your_key>.");
    }
}

