package com.tests.ui;

import com.framework.config.ConfigManager;
import com.framework.logging.Log;
import com.framework.pages.CartPage;
import com.framework.pages.CheckoutPage;
import com.framework.pages.LoginPage;
import com.framework.pages.ProductsPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * UI Smoke Flow: Login → Browse Products → Add to Cart → Checkout → Complete Order.
 */
@Epic("UI Tests")
@Feature("Smoke Flow")
public class SmokeFlowTest extends BaseUiTest {

    @Test(description = "Happy-path smoke: login, add item, checkout, complete order")
    @Story("Complete Purchase Flow")
    @Severity(SeverityLevel.CRITICAL)
    @Description("End-to-end happy path: Login with valid credentials, add an item to cart, checkout and complete the order.")
    public void testCompletePurchaseFlow() {
        ConfigManager config = ConfigManager.getInstance();
        String baseUrl = config.getUiBaseUrl();
        String username = config.getUiUsername();
        String password = config.getUiPassword();

        // Step 1: Login
        Log.info("Starting complete purchase flow test");
        LoginPage loginPage = new LoginPage(driver);
        ProductsPage productsPage = loginPage.open(baseUrl).loginAs(username, password);

        // Step 2: Verify Products page
        Assert.assertTrue(productsPage.isPageDisplayed(), "Products page should be displayed after login");
        Assert.assertEquals(productsPage.getPageTitle(), "Products");
        Log.info("Products page verified successfully");

        // Step 3: Add first item to cart
        productsPage.addItemToCartByIndex(0);
        Assert.assertEquals(productsPage.getCartBadgeCount(), 1, "Cart badge should show 1 item");
        Log.info("Item added to cart, badge count verified");

        // Step 4: Navigate to cart
        CartPage cartPage = productsPage.goToCart();
        Assert.assertTrue(cartPage.isPageDisplayed(), "Cart page should be displayed");
        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should contain 1 item");
        Log.info("Cart page verified with 1 item");

        // Step 5: Checkout
        CheckoutPage checkoutPage = cartPage.clickCheckout();
        checkoutPage.fillCheckoutInfo("John", "Doe", "12345");
        checkoutPage.clickContinue();
        Log.info("Checkout info filled and continued");

        // Step 6: Finish
        checkoutPage.clickFinish();
        String completeHeader = checkoutPage.getCompleteHeader();
        Assert.assertEquals(completeHeader, "Thank you for your order!",
                "Order completion message should match");
        Log.info("Order completed successfully: {}", completeHeader);
    }

    @Test(description = "Login with invalid credentials shows error message")
    @Story("Login Validation")
    @Severity(SeverityLevel.NORMAL)
    public void testLoginWithInvalidCredentials() {
        ConfigManager config = ConfigManager.getInstance();
        String baseUrl = config.getUiBaseUrl();

        LoginPage loginPage = new LoginPage(driver);
        loginPage.open(baseUrl);
        loginPage.enterUsername("invalid_user");
        loginPage.enterPassword("invalid_pass");
        loginPage.clickLogin();

        String errorMsg = loginPage.getErrorMessage();
        Assert.assertTrue(errorMsg.contains("Username and password do not match"),
                "Error message should indicate invalid credentials. Got: " + errorMsg);
        Log.info("Invalid login error verified: {}", errorMsg);
    }

    @Test(description = "Login with locked out user shows error")
    @Story("Login Validation")
    @Severity(SeverityLevel.NORMAL)
    public void testLoginWithLockedOutUser() {
        ConfigManager config = ConfigManager.getInstance();
        String baseUrl = config.getUiBaseUrl();

        LoginPage loginPage = new LoginPage(driver);
        loginPage.open(baseUrl);
        loginPage.enterUsername("locked_out_user");
        loginPage.enterPassword("secret_sauce");
        loginPage.clickLogin();

        String errorMsg = loginPage.getErrorMessage();
        Assert.assertTrue(errorMsg.contains("locked out"),
                "Error message should indicate user is locked out. Got: " + errorMsg);
        Log.info("Locked out error verified: {}", errorMsg);
    }
}

