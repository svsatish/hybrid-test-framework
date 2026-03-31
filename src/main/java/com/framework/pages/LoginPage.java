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
 * Page Object for SauceDemo Login Page.
 */
public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(id = "user-name")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "login-button")
    private WebElement loginButton;

    @FindBy(css = "[data-test='error']")
    private WebElement errorMessage;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigManager.getInstance().getTimeout()));
        PageFactory.initElements(driver, this);
    }

    @Step("Open login page")
    public LoginPage open(String baseUrl) {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.visibilityOf(loginButton));
        Log.info("Opened login page: {}", baseUrl);
        return this;
    }

    @Step("Enter username: {username}")
    public LoginPage enterUsername(String username) {
        wait.until(ExpectedConditions.visibilityOf(usernameInput));
        usernameInput.clear();
        usernameInput.sendKeys(username);
        Log.info("Entered username: {}", username);
        return this;
    }

    @Step("Enter password")
    public LoginPage enterPassword(String password) {
        wait.until(ExpectedConditions.visibilityOf(passwordInput));
        passwordInput.clear();
        passwordInput.sendKeys(password);
        Log.info("Entered password");
        return this;
    }

    @Step("Click login button")
    public void clickLogin() {
        loginButton.click();
        Log.info("Clicked login button");
    }

    @Step("Login with credentials: {username}")
    public ProductsPage loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        Log.info("Logged in as: {}", username);
        return new ProductsPage(driver);
    }

    public String getErrorMessage() {
        wait.until(ExpectedConditions.visibilityOf(errorMessage));
        return errorMessage.getText();
    }

    public boolean isOnLoginPage() {
        try {
            return loginButton.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}

