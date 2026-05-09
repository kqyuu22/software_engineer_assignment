const { test, expect } = require('@playwright/test');
const { loginAs } = require('./helpers/auth');

test.describe('Application - Operator Functions', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, {
      username: 'operator01',
      password: 'password123',
      expectedPath: '/operator',
      expectedHeading: 'Operator Dashboard',
    });
  });

  test('TCF-L08 Real Time View - Display Slots', async ({ page }) => {
    await expect(page.locator('h3:has-text("Real-Time Parking Slots")')).toBeVisible();
    await expect(page.locator('#slots-body')).toBeVisible();
  });

  test('TCF-L11 Manage History - Default View', async ({ page }) => {
    await expect(page.locator('h3:has-text("Ticket History")')).toBeVisible();
    await expect(page.locator('#tickets-body')).toBeVisible();
    const rowCount = await page.locator('#tickets-body tr').count();
    expect(rowCount).toBeGreaterThan(0);
  });

  test('TCF-L12 Manage History - Search by User ID', async ({ page }) => {
    await page.fill('#searchPlate', '1001');
    const responsePromise = page.waitForResponse(
      response => response.url().includes('/history/search') && response.status() === 200
    );
    await page.click('button:has-text("Search")');
    await responsePromise;
    await expect(page.locator('#tickets-body')).toBeVisible();
  });
});
