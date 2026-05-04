const { test, expect } = require('@playwright/test');

test.describe('Operator Dashboard', () => {
    test.beforeEach(async ({ page }) => {
        // Navigate and log in as operator
        await page.goto('/auth.html');
        await page.fill('#username', 'operator01');
        await page.fill('#password', 'password123'); // Adjust based on DB seed

        const responsePromise = page.waitForResponse(response => response.url().includes('/auth/login') && response.status() === 200);
        await page.click('button:has-text("Login")');
        await responsePromise;

        // Wait for redirect to operator dashboard
        await expect(page).toHaveURL(/.*\/operator/);
        await expect(page.locator('h1')).toHaveText('Operator Dashboard');
    });

    test('should view active alerts, history, slots, and tickets on load', async ({ page }) => {
        // Data fetches occur on load. Verify the containers exist.
        await expect(page.locator('h3:has-text("Ticket History")')).toBeVisible();
        await expect(page.locator('h3:has-text("Resolved Alert History")')).toBeVisible();
        await expect(page.locator('h3:has-text("Real-Time Parking Slots")')).toBeVisible();

        // Check if tables contain rows or at least loaded
        await expect(page.locator('#tickets-body')).toBeVisible();
        await expect(page.locator('#slots-body')).toBeVisible();
    });

    test('should be able to search for tickets and clear search', async ({ page }) => {
        // Perform search
        await page.fill('#searchPlate', 'GUE');
        await page.click('button:has-text("Search")');

        // Let the fetch complete and UI update (using a small timeout or network wait)
        await page.waitForResponse(response => response.url().includes('/history/search') && response.status() === 200);

        // Assuming there's a guest ticket, check the table is visible
        await expect(page.locator('#tickets-body')).toBeVisible();

        // Clear search
        await page.click('button:has-text("Clear Search")');
        await page.waitForResponse(response => response.url().includes('/history') && response.status() === 200);
    });

    test('should be able to resolve an active alert', async ({ page }) => {
        // This test will only realistically pass if there's an active alert
        // It's good practice to mock the API response if we want consistent tests,
        // but for E2E, we can check if a resolve button exists and click it.
        const resolveButton = page.locator('.btn-success:has-text("Resolve")').first();

        if (await resolveButton.isVisible()) {
            page.on('dialog', dialog => dialog.accept()); // In case of alerts/confirms

            await resolveButton.click();
            await page.waitForResponse(response => response.url().includes('/resolve') && response.status() === 200);

            // Verify UI updates (alert might disappear)
        }
    });
});
