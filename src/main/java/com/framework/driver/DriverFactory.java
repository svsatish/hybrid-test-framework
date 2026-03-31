package com.framework.driver;

import com.framework.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

/**
 * Thread-safe WebDriver factory using ThreadLocal for parallel execution support.
 */
public class DriverFactory {

    private static final Logger LOG = LogManager.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    public static WebDriver initDriver() {
        ConfigManager config = ConfigManager.getInstance();
        String browser = config.getBrowser().toLowerCase();
        boolean headless = config.isHeadless();
        int timeout = config.getTimeout();

        WebDriver driver;

        switch (browser) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions ffOptions = new FirefoxOptions();
                if (headless) ffOptions.addArguments("--headless");
                driver = new FirefoxDriver(ffOptions);
                break;
            case "chrome":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                if (headless) {
                    chromeOptions.addArguments("--headless=new");
                }
                chromeOptions.addArguments("--no-sandbox");
                chromeOptions.addArguments("--disable-dev-shm-usage");
                chromeOptions.addArguments("--window-size=1920,1080");
                chromeOptions.addArguments("--disable-gpu");
                driver = new ChromeDriver(chromeOptions);
                break;
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
        driver.manage().window().maximize();

        driverThreadLocal.set(driver);
        LOG.info("Initialized {} driver (headless={})", browser, headless);
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
            LOG.info("WebDriver quit and removed from thread-local");
        }
    }
}

