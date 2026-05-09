const { test, expect } = require('@playwright/test');
const { loginAs, logoutFromHeader } = require('./helpers/auth');

test.describe('Application - Login and Session', () => {
  test('TCF-L01 Login - Member', async ({ page }) => {
    await loginAs(page, {
      username: 'member01',
      password: '123',
      expectedPath: '/member',
      expectedHeading: 'Member Dashboard',
    });
  });

  test('TCF-L01 Login - Operator', async ({ page }) => {
    await loginAs(page, {
      username: 'operator01',
      password: 'password123',
      expectedPath: '/operator',
      expectedHeading: 'Operator Dashboard',
    });
  });

  test('TCF-L01 Login - Admin', async ({ page }) => {
    await loginAs(page, {
      username: 'admin01',
      password: 'password123',
      expectedPath: '/admin',
      expectedHeading: 'Admin Control Panel',
    });
  });

  test('TCF-L03 Logout - Manual', async ({ page }) => {
    await loginAs(page, {
      username: 'member01',
      password: '123',
      expectedPath: '/member',
      expectedHeading: 'Member Dashboard',
    });

    await logoutFromHeader(page);
    await expect(page.locator('h2')).toHaveText('System Login');
  });
});
