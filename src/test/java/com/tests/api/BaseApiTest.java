package com.tests.api;

import com.framework.config.ConfigManager;
import com.framework.logging.Log;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;

/**
 * Base class for all API tests. Sets up RestAssured base URI, content type, and Allure logging.
 */
public class BaseApiTest {

    protected RequestSpecification requestSpec;

    @BeforeClass(alwaysRun = true)
    public void apiSetup() {
        String baseUrl = ConfigManager.getInstance().getApiBaseUrl();
        RestAssured.baseURI = baseUrl;

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();

        Log.info("API Base URI set to: {}", baseUrl);
    }
}

