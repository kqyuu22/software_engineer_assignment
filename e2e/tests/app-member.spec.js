const { test, expect } = require('@playwright/test');
const { loginAs, logoutFromHeader } = require('./helpers/auth');

test.describe('Application - Member Functions', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, {
      username: 'member01',
      password: '123',
      expectedPath: '/member',
      expectedHeading: 'Member Dashboard',
    });
  });

  test('TCF-L05 View History - Active + Completed Tickets', async ({ page }) => {
    await expect(page.locator('h3:has-text("My Parking History")')).toBeVisible();
    await expect(page.locator('#history-body')).toBeVisible();
    const rowCount = await page.locator('#history-body tr').count();
    expect(rowCount).toBeGreaterThan(0);
  });

  test('TCF-L07 Logout - Manual', async ({ page }) => {
    await logoutFromHeader(page);
    await expect(page.locator('h2')).toHaveText('System Login');
  });
});
