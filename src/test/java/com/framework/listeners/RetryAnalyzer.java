package com.framework.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * TestNG retry analyzer for flaky tests.
 * Retries a failed test up to MAX_RETRY times before marking it as failed.
 * Configure max retries via system property "test.retry.max" (default: 1).
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LogManager.getLogger(RetryAnalyzer.class);
    private int retryCount = 0;

    private int getMaxRetryCount() {
        String maxRetry = System.getProperty("test.retry.max", "1");
        try {
            return Integer.parseInt(maxRetry);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    public boolean retry(ITestResult result) {
        int maxRetry = getMaxRetryCount();
        if (retryCount < maxRetry) {
            retryCount++;
            LOG.warn("Retrying test '{}' — attempt {}/{}", result.getName(), retryCount, maxRetry);
            return true;
        }
        return false;
    }
}

