const { test, expect } = require('@playwright/test');
const { loginAs } = require('./helpers/auth');
const { loginAndGetToken, authedGet } = require('./helpers/api');


test.describe('Non-functional - Security', () => {
  test('TCNF-09 Member cannot access operator route', async ({ request }) => {
    const token = await loginAndGetToken(request, { username: 'member01', password: '123' });
    const res = await authedGet(request, '/operator/slots', token);
    expect(res.status()).toBe(403);
  });

  test('TCNF-10 Operator cannot access admin route', async ({ request }) => {
    const token = await loginAndGetToken(request, { username: 'operator01', password: 'password123' });
    const res = await authedGet(request, '/admin/price', token);
    expect(res.status()).toBe(403);
  });

  test('TCNF-11 Admin cannot access operator-only route', async ({ request }) => {
    const token = await loginAndGetToken(request, { username: 'admin01', password: 'password123' });
    const res = await authedGet(request, '/operator/alerts/active', token);
    expect(res.status()).toBe(403);
  });

  test('TCNF-12 Alert ordering (security before system failure)', async ({ request }) => {
    const token = await loginAndGetToken(request, { username: 'operator01', password: 'password123' });
    const res = await authedGet(request, '/operator/alerts/active', token);
    expect(res.ok()).toBeTruthy();
    const alerts = await res.json();
    if (alerts.length < 2) {
      test.skip(true, 'Requires both SECURITY_BREACH and SYSTEM_FAILURE active alerts.');
    }
    const types = alerts.map(alert => alert.type);
    const firstSecurityIndex = types.indexOf('SECURITY_BREACH');
    const firstSystemIndex = types.indexOf('SYSTEM_FAILURE');
    expect(firstSecurityIndex).toBeGreaterThanOrEqual(0);
    expect(firstSystemIndex).toBeGreaterThanOrEqual(0);
    expect(firstSecurityIndex).toBeLessThan(firstSystemIndex);
  });
});