const { expect } = require('@playwright/test');

async function loginAs(page, { username, password, expectedPath, expectedHeading }) {
  await page.goto('/auth.html');
  await page.fill('#username', username);
  await page.fill('#password', password);

  const responsePromise = page.waitForResponse(
    response => response.url().includes('/auth/login') && response.status() === 200
  );
  await page.click('button:has-text("Login")');
  await responsePromise;

  await expect(page).toHaveURL(new RegExp(expectedPath));
  if (expectedHeading) {
    await expect(page.locator('h1')).toHaveText(expectedHeading);
  }
}

async function logoutFromHeader(page) {
  await page.click('button:has-text("Logout")');
  await expect(page).toHaveURL(/.*\/login/);
}

module.exports = {
  loginAs,
  logoutFromHeader,
};
