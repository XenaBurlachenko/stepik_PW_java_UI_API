import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;

import config.EnvConfig;

public class StatusCodeApiUiTest {
    private Playwright playwright;
    private APIRequestContext apiRequest;
    private Browser browser;
    private Page page;

    @BeforeEach
    void setup() {
        playwright = Playwright.create();

        // API контекст с базовым URL из конфига
        String baseUrl = EnvConfig.getBaseUrl();
        assertNotNull(baseUrl, "Base URL должен быть задан");
        assertFalse(baseUrl.isEmpty(), "Base URL не должен быть пустым");
        
        apiRequest = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(baseUrl)
        );

        // Настройка браузера из конфига
        BrowserType browserType;
        switch (EnvConfig.getBrowser().toLowerCase()) {
            case "firefox":
                browserType = playwright.firefox();
                break;
            case "webkit":
                browserType = playwright.webkit();
                break;
            default:
                browserType = playwright.chromium();
                break;
        }

        browser = browserType.launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(EnvConfig.isHeadless())
        );

        page = browser.newPage();
    }

    @ParameterizedTest(name = "Проверка статус-кода {0}")
    @ValueSource(ints = {200, 404})
    void testStatusCodeCombined(int statusCode) {
        // 1. API проверка
        int apiStatusCode = getApiStatusCode(statusCode);
        
        // 2. UI проверка
        int uiStatusCode = getUiStatusCode(statusCode);
        
        // 3. Сравнение результатов с assert-ами
        assertAll("Проверка статус-кода " + statusCode,
            () -> assertEquals(statusCode, apiStatusCode,
                "API должен вернуть статус-код " + statusCode),
            () -> assertEquals(statusCode, uiStatusCode,
                "UI должен вернуть статус-код " + statusCode),
            () -> assertEquals(apiStatusCode, uiStatusCode,
                "API (" + apiStatusCode + ") и UI (" + uiStatusCode + ") статус-коды должны совпадать")
        );
    }

    @Test
    void testAllStatusCodesSequentially() {
        // Тест проверяющий все коды последовательно
        int[] statusCodes = {200, 404};
        
        for (int statusCode : statusCodes) {
            int apiStatusCode = getApiStatusCode(statusCode);
            int uiStatusCode = getUiStatusCode(statusCode);
            
            // Проверка для каждого кода
            assertAll("Проверка для кода " + statusCode,
                () -> assertEquals(statusCode, apiStatusCode, 
                    "API код для " + statusCode),
                () -> assertEquals(statusCode, uiStatusCode, 
                    "UI код для " + statusCode),
                () -> assertEquals(apiStatusCode, uiStatusCode, 
                    "API и UI должны совпадать для " + statusCode)
            );
            
            // Возвращаемся на главную страницу для следующего теста
            if (statusCode != statusCodes[statusCodes.length - 1]) {
                page.goBack();
                page.waitForSelector("div.example");
            }
        }
    }

    private int getApiStatusCode(int code) {
        String endpoint = "/status_codes/" + code;
        APIResponse response = apiRequest.get(endpoint);
        int status = response.status();
        
        // Проверка статус-кода
        assertEquals(code, status, 
            "API запрос для " + code + " должен вернуть " + code);
        
        return status;
    }

    private int getUiStatusCode(int code) {
        try {
            // Навигация на страницу статус кодов
            String url = EnvConfig.getBaseUrl() + "/status_codes";
            page.navigate(url);
            
            // Ожидание загрузки страницы
            page.waitForSelector("div.example", new Page.WaitForSelectorOptions().setTimeout(10000));
            
            // Поиск ссылки
            String linkSelector = String.format("a[href*='status_codes/%d']", code);
            Locator link = page.locator(linkSelector).first();
            
            // Проверка видимости и доступности ссылки
            assertTrue(link.isVisible(), 
                "Ссылка для кода " + code + " должна быть видимой");
            assertTrue(link.isEnabled(), 
                "Ссылка для кода " + code + " должна быть доступна");
            
            // Перехват ответа и клик
            Response response = page.waitForResponse(
                res -> res.url().endsWith("/status_codes/" + code),
                () -> link.click(new Locator.ClickOptions().setTimeout(10000))
            );
            
            int status = response.status();
            
            // Проверка полученного статус-кода
            assertEquals(code, status, 
                "UI клик для кода " + code + " должен вернуть " + code);
            
            return status;
            
        } catch (Exception e) {
            fail("UI проверка упала для кода " + code + ": " + e.getMessage());
            return 0;
        }
    }

    @Test
    void testConfigIsLoaded() {
        // Проверка загрузки конфигурации
        String baseUrl = EnvConfig.getBaseUrl();
        
        assertAll("Проверка конфигурации",
            () -> assertNotNull(baseUrl, "Base URL должен быть настроен"),
            () -> assertFalse(baseUrl.isEmpty(), "Base URL не должен быть пустым"),
            () -> assertTrue(baseUrl.startsWith("http"), 
                "Base URL должен быть валидным URL")
        );
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