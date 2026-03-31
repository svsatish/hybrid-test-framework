package com.tests.ui;

import com.framework.config.ConfigManager;
import com.framework.driver.DriverFactory;
import com.framework.logging.Log;
import com.framework.recording.VideoRecorder;
import com.framework.reporting.AllureAttachmentHelper;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;

/**
 * Base class for all UI tests. Handles driver lifecycle, screenshots, and video on failure.
 */
public class BaseUiTest {

    protected WebDriver driver;
    private VideoRecorder videoRecorder;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        driver = DriverFactory.initDriver();

        if (ConfigManager.getInstance().isVideoEnabled()) {
            File videoDir = new File("build/videos");
            videoRecorder = new VideoRecorder(videoDir);
            videoRecorder.start(driver);
        }

        Log.info("=== UI Test Setup Complete ===");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        // Stop video recording first (while driver is still alive for final frames)
        File videoFile = null;
        if (videoRecorder != null) {
            videoFile = videoRecorder.stop();
        }

        if (result.getStatus() == ITestResult.FAILURE) {
            Log.error("TEST FAILED: " + result.getName());

            // Capture screenshot on failure
            if (driver != null) {
                AllureAttachmentHelper.attachScreenshot(driver, "Failure Screenshot");
                AllureAttachmentHelper.attachPageSource(driver, "Page HTML on Failure");
                Log.info("Screenshot and page source attached to report");
            }

            // Attach video for failed tests
            if (videoFile != null) {
                AllureAttachmentHelper.attachVideo(videoFile);
                Log.info("Video attached to report: {}", videoFile.getName());
            }
        } else if (videoFile != null) {
            // Delete video for passed tests to save space
            videoFile.delete();
        }

        DriverFactory.quitDriver();
        Log.info("=== UI Test Teardown Complete ===");
    }
}

