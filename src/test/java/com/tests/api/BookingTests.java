package com.tests.api;

import com.framework.config.ConfigManager;
import com.framework.logging.Log;
import com.tests.api.pojo.Booking;
import com.tests.api.pojo.BookingDates;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

/**
 * API Tests for /booking endpoint — Create, Read, Update.
 * Includes positive, negative, boundary, schema validation, and parameterized tests.
 */
@Epic("API Tests")
@Feature("Booking Endpoint")
public class BookingTests extends BaseApiTest {

    private int createdBookingId;

    // ========================= CREATE BOOKING (POST /booking) =========================

    @Test(description = "POST /booking — Create a valid booking", priority = 1)
    @Story("Create Booking")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateBookingPositive() {
        Booking booking = createDefaultBooking();

        Response response = given()
                .spec(requestSpec)
                .body(booking)
                .when()
                .post("/booking")
                .then()
                .statusCode(200)
                .body("bookingid", notNullValue())
                .body("booking.firstname", equalTo(booking.getFirstname()))
                .body("booking.lastname", equalTo(booking.getLastname()))
                .body("booking.totalprice", equalTo(booking.getTotalprice()))
                .extract().response();

        createdBookingId = response.jsonPath().getInt("bookingid");
        Log.info("Created booking with ID: {}", createdBookingId);
    }

    @Test(description = "POST /booking — Empty body returns error", priority = 2)
    @Story("Create Booking")
    @Severity(SeverityLevel.NORMAL)
    public void testCreateBookingEmptyBody() {
        Response response = given()
                .spec(requestSpec)
                .body("{}")
                .when()
                .post("/booking");

        // The API may return 500 or 200 with null fields — either way it shouldn't be a clean 200 with valid data
        int statusCode = response.getStatusCode();
        Log.info("POST /booking with empty body returned status: {}", statusCode);

        if (statusCode == 200) {
            // If API accepts empty body, validate that fields are null/default
            Assert.assertNull(response.jsonPath().getString("booking.firstname"),
                    "Firstname should be null for empty body");
        } else {
            Assert.assertTrue(statusCode == 400 || statusCode == 500,
                    "Expected 400 or 500 for empty body, got: " + statusCode);
        }
    }

    @Test(description = "POST /booking — Negative totalprice (boundary)", priority = 3)
    @Story("Create Booking")
    @Severity(SeverityLevel.NORMAL)
    public void testCreateBookingNegativePrice() {
        Booking booking = createDefaultBooking();
        booking.setTotalprice(-1);

        Response response = given()
                .spec(requestSpec)
                .body(booking)
                .when()
                .post("/booking");

        int statusCode = response.getStatusCode();
        Log.info("POST /booking with negative price returned status: {}", statusCode);

        // Document actual behavior: API may accept -1
        if (statusCode == 200) {
            int returnedPrice = response.jsonPath().getInt("booking.totalprice");
            Log.warn("API accepted negative price: {}", returnedPrice);
            Assert.assertEquals(returnedPrice, -1, "Returned price should match sent value");
        } else {
            Assert.assertTrue(statusCode == 400 || statusCode == 500,
                    "Expected 400/500 for boundary price, got: " + statusCode);
        }
    }

    @Test(description = "POST /booking — Very large totalprice (boundary)", priority = 4)
    @Story("Create Booking")
    @Severity(SeverityLevel.MINOR)
    public void testCreateBookingMaxPrice() {
        Booking booking = createDefaultBooking();
        booking.setTotalprice(Integer.MAX_VALUE);

        Response response = given()
                .spec(requestSpec)
                .body(booking)
                .when()
                .post("/booking");

        int statusCode = response.getStatusCode();
        Log.info("POST /booking with MAX_VALUE price returned status: {}", statusCode);

        if (statusCode == 200) {
            Assert.assertNotNull(response.jsonPath().get("bookingid"), "Booking ID should be returned");
        }
    }

    // ========================= Parameterized CREATE from JSON =========================

    @DataProvider(name = "bookingData")
    public Object[][] bookingDataProvider() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("testdata/bookings.json")) {
            List<Map<String, Object>> dataList = mapper.readValue(is, new TypeReference<List<Map<String, Object>>>() {});

            Object[][] data = new Object[dataList.size()][1];
            for (int i = 0; i < dataList.size(); i++) {
                data[i][0] = dataList.get(i);
            }
            return data;
        }
    }

    @Test(description = "POST /booking — Parameterized from JSON data", dataProvider = "bookingData", priority = 5)
    @Story("Create Booking - Parameterized")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateBookingParameterized(Map<String, Object> bookingData) {
        int expectedStatus = bookingData.containsKey("expectedStatus")
                ? (int) bookingData.get("expectedStatus") : 200;

        // Remove meta fields before sending
        Map<String, Object> payload = new java.util.HashMap<>(bookingData);
        payload.remove("expectedStatus");
        payload.remove("testDescription");

        String desc = bookingData.containsKey("testDescription")
                ? (String) bookingData.get("testDescription") : "Parameterized booking test";
        Log.info("Running parameterized test: {}", desc);

        Response response = given()
                .spec(requestSpec)
                .body(payload)
                .when()
                .post("/booking");

        int actualStatus = response.getStatusCode();
        Log.info("Parameterized test '{}' returned status: {}", desc, actualStatus);

        if (expectedStatus == 200) {
            Assert.assertEquals(actualStatus, 200, "Expected success for: " + desc);
            Assert.assertNotNull(response.jsonPath().get("bookingid"), "Should return booking ID");
        } else {
            Assert.assertTrue(actualStatus == expectedStatus || actualStatus == 500,
                    "Expected " + expectedStatus + " or 500 for: " + desc + ", got: " + actualStatus);
        }
    }

    // ========================= GET BOOKING (GET /booking/{id}) =========================

    @Test(description = "GET /booking/{id} — Retrieve existing booking", priority = 6,
            dependsOnMethods = "testCreateBookingPositive")
    @Story("Get Booking")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetBookingPositive() {
        given()
                .spec(requestSpec)
                .pathParam("id", createdBookingId)
                .when()
                .get("/booking/{id}")
                .then()
                .statusCode(200)
                .body("firstname", notNullValue())
                .body("lastname", notNullValue())
                .body("totalprice", notNullValue())
                .body("bookingdates", notNullValue())
                .body("bookingdates.checkin", notNullValue())
                .body("bookingdates.checkout", notNullValue());

        Log.info("GET /booking/{} returned valid booking", createdBookingId);
    }

    @Test(description = "GET /booking/{id} — Schema validation", priority = 7,
            dependsOnMethods = "testCreateBookingPositive")
    @Story("Get Booking")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetBookingSchemaValidation() {
        given()
                .spec(requestSpec)
                .pathParam("id", createdBookingId)
                .when()
                .get("/booking/{id}")
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/booking-schema.json"));

        Log.info("GET /booking/{} passed JSON schema validation", createdBookingId);
    }

    @Test(description = "GET /booking/{id} — Non-existent ID returns 404", priority = 8)
    @Story("Get Booking")
    @Severity(SeverityLevel.NORMAL)
    public void testGetBookingNotFound() {
        given()
                .spec(requestSpec)
                .pathParam("id", 999999999)
                .when()
                .get("/booking/{id}")
                .then()
                .statusCode(404);

        Log.info("GET /booking/999999999 correctly returned 404");
    }

    @Test(description = "GET /booking/{id} — Invalid ID format (boundary)", priority = 9)
    @Story("Get Booking")
    @Severity(SeverityLevel.MINOR)
    public void testGetBookingInvalidId() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/booking/abc");

        int statusCode = response.getStatusCode();
        Log.info("GET /booking/abc returned status: {}", statusCode);
        Assert.assertTrue(statusCode == 400 || statusCode == 404 || statusCode == 500,
                "Expected error status for invalid ID, got: " + statusCode);
    }

    // ========================= GET BOOKING IDS (GET /booking) =========================

    @Test(description = "GET /booking — List all booking IDs", priority = 10)
    @Story("List Bookings")
    @Severity(SeverityLevel.NORMAL)
    public void testGetAllBookingIds() {
        given()
                .spec(requestSpec)
                .when()
                .get("/booking")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThan(0)))
                .body("[0].bookingid", notNullValue());

        Log.info("GET /booking returned list of booking IDs");
    }

    @Test(description = "GET /booking — Filter by name", priority = 11)
    @Story("List Bookings")
    @Severity(SeverityLevel.NORMAL)
    public void testGetBookingIdsByName() {
        // First create a booking with known name
        Booking booking = createDefaultBooking();
        booking.setFirstname("FilterTestName");
        booking.setLastname("FilterTestLast");

        given()
                .spec(requestSpec)
                .body(booking)
                .when()
                .post("/booking")
                .then()
                .statusCode(200);

        // Now filter
        Response response = given()
                .spec(requestSpec)
                .queryParam("firstname", "FilterTestName")
                .queryParam("lastname", "FilterTestLast")
                .when()
                .get("/booking")
                .then()
                .statusCode(200)
                .extract().response();

        List<?> bookings = response.jsonPath().getList("$");
        Assert.assertTrue(bookings.size() >= 1,
                "Should find at least 1 booking for FilterTestName/FilterTestLast");
        Log.info("Filter by name returned {} bookings", bookings.size());
    }

    // ========================= UPDATE BOOKING (PUT /booking/{id}) =========================

    @Test(description = "PUT /booking/{id} — Update booking with auth", priority = 12,
            dependsOnMethods = "testCreateBookingPositive")
    @Story("Update Booking")
    @Severity(SeverityLevel.CRITICAL)
    public void testUpdateBookingPositive() {
        // Get auth token first
        String token = getAuthToken();

        Booking updated = createDefaultBooking();
        updated.setFirstname("UpdatedFirstName");
        updated.setLastname("UpdatedLastName");
        updated.setTotalprice(999);

        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", createdBookingId)
                .body(updated)
                .when()
                .put("/booking/{id}")
                .then()
                .statusCode(200)
                .body("firstname", equalTo("UpdatedFirstName"))
                .body("lastname", equalTo("UpdatedLastName"))
                .body("totalprice", equalTo(999));

        Log.info("PUT /booking/{} updated successfully", createdBookingId);
    }

    @Test(description = "PUT /booking/{id} — Update without auth returns 403", priority = 13,
            dependsOnMethods = "testCreateBookingPositive")
    @Story("Update Booking")
    @Severity(SeverityLevel.CRITICAL)
    public void testUpdateBookingWithoutAuth() {
        Booking updated = createDefaultBooking();
        updated.setFirstname("Unauthorized");

        Response response = given()
                .spec(requestSpec)
                .pathParam("id", createdBookingId)
                .body(updated)
                .when()
                .put("/booking/{id}");

        int statusCode = response.getStatusCode();
        Log.info("PUT /booking without auth returned status: {}", statusCode);
        Assert.assertTrue(statusCode == 403 || statusCode == 401,
                "Expected 401 or 403 without auth, got: " + statusCode);
    }

    @Test(description = "PUT /booking/{id} — Update non-existent booking", priority = 14)
    @Story("Update Booking")
    @Severity(SeverityLevel.NORMAL)
    public void testUpdateBookingNotFound() {
        String token = getAuthToken();
        Booking updated = createDefaultBooking();

        Response response = given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", 999999999)
                .body(updated)
                .when()
                .put("/booking/{id}");

        int statusCode = response.getStatusCode();
        Log.info("PUT /booking/999999999 returned status: {}", statusCode);
        Assert.assertTrue(statusCode == 404 || statusCode == 405 || statusCode == 500,
                "Expected error for non-existent booking update, got: " + statusCode);
    }

    // ========================= AUTH (POST /auth) =========================

    @Test(description = "POST /auth — Create token with valid credentials", priority = 15)
    @Story("Authentication")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateTokenPositive() {
        ConfigManager config = ConfigManager.getInstance();
        String username = config.get("api.auth.username", "admin");
        String password = config.get("api.auth.password", "password123");

        given()
                .spec(requestSpec)
                .body("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}")
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .body("token", notNullValue());

        Log.info("POST /auth returned valid token");
    }

    @Test(description = "POST /auth — Invalid credentials", priority = 16)
    @Story("Authentication")
    @Severity(SeverityLevel.NORMAL)
    public void testCreateTokenInvalidCredentials() {
        given()
                .spec(requestSpec)
                .body("{\"username\": \"wronguser\", \"password\": \"wrongpass\"}")
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .body("reason", equalTo("Bad credentials"));

        Log.info("POST /auth with bad credentials returned expected error");
    }

    @Test(description = "POST /auth — Empty credentials (boundary)", priority = 17)
    @Story("Authentication")
    @Severity(SeverityLevel.MINOR)
    public void testCreateTokenEmptyCredentials() {
        Response response = given()
                .spec(requestSpec)
                .body("{\"username\": \"\", \"password\": \"\"}")
                .when()
                .post("/auth");

        int statusCode = response.getStatusCode();
        Log.info("POST /auth with empty creds returned status: {}", statusCode);
        Assert.assertEquals(statusCode, 200, "Status should be 200");
        Assert.assertEquals(response.jsonPath().getString("reason"), "Bad credentials",
                "Should return Bad credentials for empty creds");
    }

    // ========================= Helpers =========================

    private String getAuthToken() {
        ConfigManager config = ConfigManager.getInstance();
        String username = config.get("api.auth.username", "admin");
        String password = config.get("api.auth.password", "password123");
        return given()
                .spec(requestSpec)
                .body("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}")
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }

    private Booking createDefaultBooking() {
        return new Booking(
                "James",
                "Brown",
                150,
                true,
                new BookingDates("2026-06-01", "2026-06-10"),
                "Breakfast"
        );
    }
}

