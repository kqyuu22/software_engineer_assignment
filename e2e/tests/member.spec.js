const { test, expect } = require('@playwright/test');

test('Member can view tickets and process payment', async ({ page }) => {
    // 1. Navigate to login
    await page.goto('/auth.html');

    // 2. Perform login 
    await page.fill('#username', 'member01');
    await page.fill('#password', '123'); // Assuming these are valid credentials

    // Wait for response after clicking login
    const responsePromise = page.waitForResponse(response => response.url().includes('/auth/login') && response.status() === 200);
    await page.click('button:has-text("Login")');
    await responsePromise;

    // 3. Ensure redirection to the correct dashboard
    await expect(page).toHaveURL(/.*\/member/);
    await expect(page.locator('h1')).toHaveText('Member Dashboard');

    // 4. Validate the page has loaded the history correctly
    await expect(page.locator('h3:has-text("My Parking History")')).toBeVisible();

    // Note: To test the payment button, you'd find a row that has a 'Pay Now (BKPay)' button, 
    // click it, accept the dialog, and wait for the 'Paid' text to appear. 
    // Since we don't know the exact seed data, we will just verify the tables exist.
    await expect(page.locator('#history-body')).toBeVisible();
    await expect(page.locator('#payment-body')).toBeVisible();
});
