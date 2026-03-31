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

/**
 * Page Object for SauceDemo Checkout Pages (Step One, Step Two, Complete).
 */
public class CheckoutPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Checkout Step One
    @FindBy(css = "[data-test='firstName']")
    private WebElement firstNameInput;

    @FindBy(css = "[data-test='lastName']")
    private WebElement lastNameInput;

    @FindBy(css = "[data-test='postalCode']")
    private WebElement postalCodeInput;

    @FindBy(css = "[data-test='continue']")
    private WebElement continueButton;

    // Checkout Step Two
    @FindBy(css = "[data-test='finish']")
    private WebElement finishButton;

    // Checkout Complete
    @FindBy(css = "[data-test='complete-header']")
    private WebElement completeHeader;

    @FindBy(css = "[data-test='title']")
    private WebElement pageTitle;

    public CheckoutPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigManager.getInstance().getTimeout()));
        PageFactory.initElements(driver, this);
    }

    @Step("Fill checkout info: {firstName} {lastName} {zip}")
    public CheckoutPage fillCheckoutInfo(String firstName, String lastName, String zip) {
        wait.until(ExpectedConditions.visibilityOf(firstNameInput));
        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);
        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);
        postalCodeInput.clear();
        postalCodeInput.sendKeys(zip);
        Log.info("Filled checkout info: {} {} {}", firstName, lastName, zip);
        return this;
    }

    @Step("Click continue on checkout")
    public CheckoutPage clickContinue() {
        wait.until(ExpectedConditions.elementToBeClickable(continueButton));
        continueButton.click();
        Log.info("Clicked continue on checkout step one");
        // Re-init elements for step two page
        PageFactory.initElements(driver, this);
        return this;
    }

    @Step("Click finish to complete order")
    public CheckoutPage clickFinish() {
        wait.until(ExpectedConditions.elementToBeClickable(finishButton));
        finishButton.click();
        Log.info("Clicked finish to complete order");
        // Re-init for complete page
        PageFactory.initElements(driver, this);
        return this;
    }

    @Step("Get order completion header")
    public String getCompleteHeader() {
        wait.until(ExpectedConditions.visibilityOf(completeHeader));
        return completeHeader.getText();
    }

    public String getPageTitle() {
        wait.until(ExpectedConditions.visibilityOf(pageTitle));
        return pageTitle.getText();
    }
}

