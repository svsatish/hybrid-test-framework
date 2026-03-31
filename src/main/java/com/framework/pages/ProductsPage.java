package com.framework.pages;

import com.framework.config.ConfigManager;
import com.framework.logging.Log;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object for SauceDemo Products (Inventory) Page.
 */
public class ProductsPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(css = "[data-test='title']")
    private WebElement pageTitle;

    @FindBy(css = "[data-test='inventory-item']")
    private List<WebElement> inventoryItems;

    @FindBy(css = "[data-test='shopping-cart-link']")
    private WebElement cartLink;

    @FindBy(css = "[data-test='shopping-cart-badge']")
    private WebElement cartBadge;

    public ProductsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigManager.getInstance().getTimeout()));
        PageFactory.initElements(driver, this);
    }

    @Step("Verify products page is displayed")
    public boolean isPageDisplayed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(pageTitle));
            return pageTitle.getText().equalsIgnoreCase("Products");
        } catch (Exception e) {
            return false;
        }
    }

    public String getPageTitle() {
        wait.until(ExpectedConditions.visibilityOf(pageTitle));
        return pageTitle.getText();
    }

    public int getInventoryItemCount() {
        return inventoryItems.size();
    }

    @Step("Add item to cart by index: {index}")
    public ProductsPage addItemToCartByIndex(int index) {
        WebElement item = inventoryItems.get(index);
        WebElement addButton = item.findElement(By.cssSelector("button[id^='add-to-cart']"));
        wait.until(ExpectedConditions.elementToBeClickable(addButton));
        addButton.click();
        Log.info("Added item at index {} to cart", index);
        return this;
    }

    @Step("Get cart badge count")
    public int getCartBadgeCount() {
        try {
            wait.until(ExpectedConditions.visibilityOf(cartBadge));
            return Integer.parseInt(cartBadge.getText());
        } catch (Exception e) {
            return 0;
        }
    }

    @Step("Navigate to cart")
    public CartPage goToCart() {
        wait.until(ExpectedConditions.elementToBeClickable(cartLink));
        cartLink.click();
        Log.info("Navigated to cart");
        return new CartPage(driver);
    }
}

