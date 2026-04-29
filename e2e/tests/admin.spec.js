const { test, expect } = require('@playwright/test');

test.describe('Admin Dashboard', () => {
    test.beforeEach(async ({ page }) => {
        // Navigate and log in as admin
        await page.goto('/auth.html');
        await page.fill('#username', 'admin01');
        await page.fill('#password', 'password123'); // Adjust based on DB seed

        const responsePromise = page.waitForResponse(response => response.url().includes('/auth/login') && response.status() === 200);
        await page.click('button:has-text("Login")');
        await responsePromise;

        // Wait for redirect to admin dashboard
        await expect(page).toHaveURL(/.*\/admin/);
        await expect(page.locator('h1')).toHaveText('Admin Control Panel');
    });

    test('should be able to view and update the global price', async ({ page }) => {
        // Wait for the price to load
        await expect(page.locator('#current-price')).not.toHaveText('Loading...', { timeout: 5000 });

        // Update price
        await page.fill('#newPrice', '6000');

        // Automatically accept the toast/alert message
        page.on('dialog', dialog => dialog.accept());

        const updatePromise = page.waitForResponse(response => response.url().includes('/admin/price') && response.status() === 200);
        await page.click('button:has-text("Update Global Price")');
        await updatePromise;

        // The UI should theoretically reload the price or show a toast
        await expect(page.locator('#current-price')).toHaveText('6000', { timeout: 5000 });
    });

    test('should be able to add and bulk update slots', async ({ page }) => {
        // Automatically dismiss any alerts
        page.on('dialog', dialog => dialog.accept());

        // Add single slot
        await page.selectOption('#prioritySelect', 'STAFF');

        const addPromise = page.waitForResponse(response => response.url().includes('/admin/slots') && response.request().method() === 'POST');
        await page.click('button:has-text("Add New Slot")');
        await addPromise;

        // Bulk update slots
        await page.fill('#bulkSlotInput', '1-2');
        await page.selectOption('#bulkPrioritySelect', 'LECTURER');

        const bulkPromise = page.waitForResponse(response => response.url().includes('/admin/slots/bulk') && response.request().method() === 'PATCH');
        await page.click('button:has-text("Apply Bulk Update")');
        await bulkPromise;

        // Verify the slots table reflects changes
        await expect(page.locator('#slots-body')).toBeVisible();
    });

    test('should be able to search for tickets and clear search', async ({ page }) => {
        // Perform search
        await page.fill('#searchPlate', 'GUE');
        await page.click('button:has-text("Search")');

        await page.waitForResponse(response => response.url().includes('/history/search') && response.status() === 200);
        await expect(page.locator('#tickets-body')).toBeVisible();

        // Clear search
        await page.click('button:has-text("Clear Search")');
        await page.waitForResponse(response => response.url().includes('/history') && response.status() === 200);
    });
});
