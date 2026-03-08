package com.eottabom.letmecode.example.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class PlaywrightTests {

	@Test
	void test() {
		try (Playwright playwright = Playwright.create()) {
			Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
			BrowserContext context = browser.newContext();
			Page page = context.newPage();

			page.navigate("https://the-internet.herokuapp.com/login");

			page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username")).fill("tomsmith");
			page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password"))
				.fill("SuperSecretPassword!");
//			page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password")).press("Enter");
			page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(" Login")).click();

			assertThat(page.getByText("You logged into a secure area")).isVisible();
		}
	}

}