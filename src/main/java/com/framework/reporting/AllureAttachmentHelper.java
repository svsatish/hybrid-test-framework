package com.framework.reporting;

import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Utility class for attaching artifacts to Allure reports.
 */
public class AllureAttachmentHelper {

    private AllureAttachmentHelper() {
    }

    public static void attachScreenshot(WebDriver driver, String name) {
        if (driver instanceof TakesScreenshot) {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(screenshot), ".png");
        }
    }

    public static void attachVideo(File videoFile) {
        if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
            try {
                byte[] videoBytes = Files.readAllBytes(videoFile.toPath());
                Allure.addAttachment("Test Video", "video/mp4",
                        new ByteArrayInputStream(videoBytes), ".mp4");
            } catch (IOException e) {
                // best-effort
            }
        }
    }

    public static void attachText(String name, String content) {
        Allure.addAttachment(name, "text/plain", content);
    }

    public static void attachPageSource(WebDriver driver, String name) {
        String pageSource = driver.getPageSource();
        Allure.addAttachment(name, "text/html", pageSource);
    }
}

