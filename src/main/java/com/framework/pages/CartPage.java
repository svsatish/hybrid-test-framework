package com.framework.pages;

import com.framework.config.ConfigManager;
import com.framework.logging.Log;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object for SauceDemo Cart Page.
 */
public class CartPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(css = "[data-test='title']")
    private WebElement pageTitle;

    @FindBy(css = "[data-test='inventory-item']")
    private List<WebElement> cartItems;

    @FindBy(css = "[data-test='checkout']")
    private WebElement checkoutButton;

    @FindBy(css = "[data-test='continue-shopping']")
    private WebElement continueShoppingButton;

    public CartPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigManager.getInstance().getTimeout()));
        PageFactory.initElements(driver, this);
    }

    @Step("Verify cart page is displayed")
    public boolean isPageDisplayed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(pageTitle));
            return pageTitle.getText().equalsIgnoreCase("Your Cart");
        } catch (Exception e) {
            return false;
        }
    }

    public int getCartItemCount() {
        return cartItems.size();
    }

    @Step("Click checkout")
    public CheckoutPage clickCheckout() {
        wait.until(ExpectedConditions.elementToBeClickable(checkoutButton));
        checkoutButton.click();
        Log.info("Clicked checkout button");
        return new CheckoutPage(driver);
    }
}

