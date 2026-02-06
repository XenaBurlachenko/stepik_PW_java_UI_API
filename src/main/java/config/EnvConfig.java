package config;

import java.io.InputStream;
import java.util.Properties;

public class EnvConfig {
    private static Properties properties;
    
    static {
        properties = new Properties();
        try {
            InputStream input = EnvConfig.class.getClassLoader()
                    .getResourceAsStream("config.properties");
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception e) {
            System.out.println("Используем значения по умолчанию для конфигурации");
        }
    }
    
    public static String getBaseUrl() {
        return properties.getProperty("base.url", "https://the-internet.herokuapp.com");
    }
    
    public static boolean isHeadless() {
        return Boolean.parseBoolean(properties.getProperty("headless", "true"));
    }
    
    public static String getBrowser() {
        return properties.getProperty("browser", "chromium");
    }
}