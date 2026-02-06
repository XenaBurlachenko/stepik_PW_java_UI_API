import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;

public class StatusCodeApiUiTest {
    private Playwright playwright;
    private APIRequestContext apiRequest;
    private Browser browser;
    private Page page;

    @BeforeEach
    void setup() {
        playwright = Playwright.create();

        // Настройка API контекста
        apiRequest = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL("https://the-internet.herokuapp.com")
        );

        // Настройка браузера
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
        );

        page = browser.newPage();
        
        // Навигация на страницу статус кодов один раз
        page.navigate("https://the-internet.herokuapp.com/status_codes");
        page.waitForSelector("div.example");
    }

    @Test
    void testStatusCodesCombined() {
        int[] statusCodes = {200, 404};
        
        for (int code : statusCodes) {
            int apiStatusCode = getApiStatusCode(code);
            int uiStatusCode = getUiStatusCode(code);
            
            // Проверяем, что API и UI возвращают одинаковые статус-коды
            assertEquals(apiStatusCode, uiStatusCode, 
                    "API и UI статус-коды должны совпадать для кода " + code);
            
            // Проверяем, что полученный статус-код соответствует ожидаемому
            assertEquals(code, apiStatusCode, 
                    "API статус-код должен быть " + code);
            assertEquals(code, uiStatusCode, 
                    "UI статус-код должен быть " + code);
            
            // Возвращаемся на главную страницу статус кодов для следующего теста
            if (code != statusCodes[statusCodes.length - 1]) {
                page.goBack();
                page.waitForSelector("div.example");
            }
        }
    }

    @Test
    void testStatusCode200() {
        int expectedCode = 200;
        
        int apiStatusCode = getApiStatusCode(expectedCode);
        int uiStatusCode = getUiStatusCode(expectedCode);
        
        // Проверяем API ответ
        assertAll("API тест для статус-кода 200",
            () -> assertEquals(expectedCode, apiStatusCode, 
                "API должен вернуть статус 200"),
            () -> assertTrue(apiStatusCode >= 200 && apiStatusCode < 300,
                "API статус-код 200 должен быть в диапазоне 2xx")
        );
        
        // Проверяем UI ответ
        assertAll("UI тест для статус-кода 200",
            () -> assertEquals(expectedCode, uiStatusCode, 
                "UI должен вернуть статус 200"),
            () -> assertEquals(apiStatusCode, uiStatusCode,
                "API и UI статус-коды должны совпадать для 200")
        );
    }

    @Test
    void testStatusCode404() {
        int expectedCode = 404;
        
        int apiStatusCode = getApiStatusCode(expectedCode);
        int uiStatusCode = getUiStatusCode(expectedCode);
        
        // Проверяем API ответ
        assertAll("API тест для статус-кода 404",
            () -> assertEquals(expectedCode, apiStatusCode, 
                "API должен вернуть статус 404"),
            () -> assertTrue(apiStatusCode >= 400 && apiStatusCode < 500,
                "API статус-код 404 должен быть в диапазоне 4xx")
        );
        
        // Проверяем UI ответ
        assertAll("UI тест для статус-кода 404",
            () -> assertEquals(expectedCode, uiStatusCode, 
                "UI должен вернуть статус 404"),
            () -> assertEquals(apiStatusCode, uiStatusCode,
                "API и UI статус-коды должны совпадать для 404")
        );
    }

    private int getApiStatusCode(int code) {
        APIResponse response = apiRequest.get("/status_codes/" + code);
        return response.status();
    }

    private int getUiStatusCode(int code) {
        try {
            // Находим ссылку с нужным кодом статуса
            Locator link = page.locator("a[href='status_codes/" + code + "']").first();
            
            // Проверяем, что ссылка видима и доступна
            assertTrue(link.isVisible(), "Ссылка для кода " + code + " должна быть видимой");
            assertTrue(link.isEnabled(), "Ссылка для кода " + code + " должна быть доступна");
            
            // Ожидаем ответ от сервера после клика
            Response response = page.waitForResponse(
                    res -> res.url().endsWith("/status_codes/" + code),
                    () -> link.click(new Locator.ClickOptions().setTimeout(15000))
            );
            
            return response.status();
        } catch (Exception e) {
            fail("Ошибка при получении UI статус-кода для " + code + ": " + e.getMessage());
            return 0; // Этот return никогда не выполнится из-за fail выше
        }
    }

    @AfterEach
    void teardown() {
        if (apiRequest != null) {
            apiRequest.dispose();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}