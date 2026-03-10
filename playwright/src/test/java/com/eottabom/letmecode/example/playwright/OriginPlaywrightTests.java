package com.eottabom.letmecode.example.playwright;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@UsePlaywright
class OriginPlaywrightTests {

	@Test
	void test(Page page) {
		page.navigate("https://the-internet.herokuapp.com/login");
		page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username")).click();
		page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username")).fill("tomsmith");
		page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username")).press("Tab");
		page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password")).fill("SuperSecretPassword!");
//		page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password")).press("Enter");
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(" Login")).click();
		assertThat(page.getByText("You logged into a secure area")).isVisible();
	}
}
