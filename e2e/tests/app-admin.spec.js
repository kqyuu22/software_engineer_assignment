const { test, expect } = require('@playwright/test');
const { loginAs } = require('./helpers/auth');

test.describe('Application - Admin Functions', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, {
      username: 'admin01',
      password: 'password123',
      expectedPath: '/admin',
      expectedHeading: 'Admin Control Panel',
    });
  });

  test('TCF-L13 Adjust Price - Successful Update', async ({ page }) => {
    await expect(page.locator('#price-body')).not.toContainText('Loading...', { timeout: 5000 });

    await page.selectOption('#pricePriority', 'STUDENT');
    await page.fill('#newPrice', '6000');

    page.on('dialog', dialog => dialog.accept());

    const updatePromise = page.waitForResponse(
      response => response.url().includes('/admin/price') && response.status() === 200
    );
    await page.click('button:has-text("Update Price")');
    await updatePromise;

    await expect(page.locator('#price-body')).toContainText('6000', { timeout: 5000 });
  });

  test('TCF-L15 Set Priority - Single Slot/Bulk Update', async ({ page }) => {
    page.on('dialog', dialog => dialog.accept());

    await page.fill('#bulkSlotInput', '1-2');
    await page.selectOption('#bulkPrioritySelect', 'LECTURER');

    const bulkPromise = page.waitForResponse(
      response => response.url().includes('/admin/slots/bulk') && response.request().method() === 'PATCH'
    );
    await page.click('button:has-text("Apply Bulk Update")');
    await bulkPromise;

    await expect(page.locator('#slots-body')).toBeVisible();
  });
});
