const { test, expect } = require('@playwright/test');

test.describe('Global JSON Format Validation', () => {
    let adminToken, memberToken, operatorToken;

    test.beforeAll(async ({ request }) => {
        // Log in as different roles to get necessary tokens
        const adminRes = await request.post('/auth/login', { data: { username: 'admin01', password: 'password123' } });
        adminToken = (await adminRes.json()).token;

        const memberRes = await request.post('/auth/login', { data: { username: 'member01', password: '123' } });
        memberToken = (await memberRes.json()).token;

        const operatorRes = await request.post('/auth/login', { data: { username: 'operator01', password: 'password123' } });
        operatorToken = (await operatorRes.json()).token;
    });

    // Helper to validate JSON headers and parseability
    async function assertJson(response) {
        expect(response.headers()['content-type']).toContain('application/json');
        const body = await response.json();
        expect(body).toBeDefined();
        return body;
    }

    test('AuthController: Login returns JSON', async ({ request }) => {
        const res = await request.post('/auth/login', { data: { username: 'admin01', password: 'password123' } });
        await assertJson(res); //[cite: 6]
    });

    test('AdminController: All routes return JSON', async ({ request }) => {
        const routes = ['/admin/history', '/admin/slots', '/admin/price']; //
        for (const route of routes) {
            const res = await request.get(route, { headers: { 'Authorization': adminToken } });
            await assertJson(res);
        }
    });

    test('MemberController: History and Payments return JSON', async ({ request }) => {
        const resHistory = await request.get('/member/history', { headers: { 'Authorization': memberToken } });
        await assertJson(resHistory); //

        const resPay = await request.get('/member/payment', { headers: { 'Authorization': memberToken } });
        await assertJson(resPay); //[cite: 7]
    });

    test('OperatorController: Alerts and Slots return JSON', async ({ request }) => {
        const routes = ['/operator/slots', '/operator/history', '/operator/alerts/active']; //
        for (const route of routes) {
            const res = await request.get(route, { headers: { 'Authorization': operatorToken } });
            await assertJson(res);
        }
    });

    test('SimulationApiController: IoT endpoints return JSON', async ({ request }) => {
        const resSlots = await request.get('/api/simulation/slots');
        await assertJson(resSlots); //

        const resSigns = await request.get('/api/simulation/signs');
        await assertJson(resSigns); //[cite: 9]
    });
});