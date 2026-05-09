const { test, expect } = require('@playwright/test');

test.describe('Functional - Entry and Exit', () => {
  test.beforeEach(async ({ request }) => {
    await request.post('/api/simulation/reset-all');
  });

  test.afterEach(async ({ request }) => {
    await request.post('/api/simulation/reset-all');
  });

  test('TCF-E01 Successful Entry - University Member', async ({ request }) => {
    const userId = 2002;

    await request.get(`/api/simulation/scan-card?param=${userId}&read=true&isGuest=false`);
    const plateRes = await request.get(`/api/simulation/scan-plate?mode=random`);
    const plateData = await plateRes.json();
    const licensePlate = plateData.licensePlate;

    const entranceRes = await request.post(`/api/simulation/entrance`);
    expect(entranceRes.ok()).toBeTruthy();
    const entranceData = await entranceRes.json();
    expect(entranceData.success).toBe(true);
    expect(entranceData.ticketId).toBeDefined();

    const activeTickets = await request.get(`/api/simulation/active-tickets`);
    const tickets = await activeTickets.json();
    const ticket = tickets.find(t => t.ticketId === entranceData.ticketId);
    expect(ticket).toBeDefined();
    expect(ticket.holderIdentifier).toBe(String(userId));
    expect(ticket.licensePlate).toBe(licensePlate);
  });

  test('TCF-E02 Successful Entry - Visitor', async ({ request }) => {
    await request.get(`/api/simulation/scan-card?param=1&read=true&isGuest=true`);
    const plateRes = await request.get(`/api/simulation/scan-plate?mode=random`);
    const plateData = await plateRes.json();
    const licensePlate = plateData.licensePlate;

    const entranceRes = await request.post(`/api/simulation/entrance`);
    expect(entranceRes.ok()).toBeTruthy();
    const entranceData = await entranceRes.json();
    expect(entranceData.success).toBe(true);
    expect(entranceData.ticketId).toBeDefined();

    const activeTickets = await request.get(`/api/simulation/active-tickets`);
    const tickets = await activeTickets.json();
    const ticket = tickets.find(t => t.ticketId === entranceData.ticketId);
    expect(ticket).toBeDefined();
    expect(ticket.ticketType).toBe('GUEST');
    expect(ticket.licensePlate).toBe(licensePlate);
  });

  test('TCF-E06 Denied Entry - No Spot Available', async ({ request }) => {
    await request.post(`/api/simulation/slots/fill-all`);

    const entranceRes = await request.post(`/api/simulation/entrance`);
    expect(entranceRes.status()).toBe(400);
    const entranceData = await entranceRes.json();
    expect(entranceData.error).toContain('full');

    const gateStatus = await request.get(`/api/simulation/gate-status`);
    const gateData = await gateStatus.json();
    expect(gateData.isOpen).toBe(false);
  });

  test('TCF-E10 Denied Entry - Duplicate Plate', async ({ request }) => {
    const userId = 1002;

    await request.get(`/api/simulation/scan-card?param=${userId}&read=true&isGuest=false`);
    await request.get(`/api/simulation/scan-plate?mode=correct&expectedPlate=ABC123`);
    const entrance1 = await request.post(`/api/simulation/entrance`);
    expect(entrance1.ok()).toBeTruthy();

    const userId2 = 1003;
    await request.get(`/api/simulation/scan-card?param=${userId2}&read=true&isGuest=false`);
    await request.get(`/api/simulation/scan-plate?mode=correct&expectedPlate=ABC123`);

    const entranceRes = await request.post(`/api/simulation/entrance`);
    expect(entranceRes.status()).toBe(400);
    const entranceData = await entranceRes.json();
    expect(entranceData.error).toContain('Plate already exists');

    const gateStatus = await request.get(`/api/simulation/gate-status`);
    const gateData = await gateStatus.json();
    expect(gateData.isOpen).toBe(false);
  });

  test('TCF-X01 Successful Exit - University Member', async ({ request }) => {
    const userId = 1004;

    await request.get(`/api/simulation/scan-card?param=${userId}&read=true&isGuest=false`);
    const plateRes = await request.get(`/api/simulation/scan-plate?mode=random`);
    const plateData = await plateRes.json();
    const licensePlate = plateData.licensePlate;

    const entranceRes = await request.post(`/api/simulation/entrance`);
    const entranceData = await entranceRes.json();
    expect(entranceData.success).toBe(true);
    const ticketId = entranceData.ticketId;

    const slots = await request.get(`/api/simulation/slots`);
    const slotData = await slots.json();
    const ticketSlot = slotData.find(s => s.slotId === entranceData.slotId);
    const price = ticketSlot ? ticketSlot.price : null;

    if (price && price > 0) {
      await request.post(`/api/simulation/pay?ticketType=SSO&userId=${userId}&price=${price}&paidDirectly=false`);
    }

    const exitRes = await request.post(`/api/simulation/exit?userId=${userId}&licensePlate=${licensePlate}`);
    expect(exitRes.ok()).toBeTruthy();
    const exitData = await exitRes.json();
    expect(exitData.success).toBe(true);

    const activeTickets = await request.get(`/api/simulation/active-tickets`);
    const tickets = await activeTickets.json();
    const ticket = tickets.find(t => t.ticketId === ticketId);
    expect(ticket).toBeUndefined();
  });

  test('TCF-X07 Denied Exit - No Active Ticket', async ({ request }) => {
    const exitRes = await request.post(`/api/simulation/exit?userId=9999&licensePlate=NOTEXIST`);
    expect(exitRes.status()).toBe(400);
    const exitData = await exitRes.json();
    expect(exitData.error).toContain('No active ticket');

    const gateStatus = await request.get(`/api/simulation/gate-status`);
    const gateData = await gateStatus.json();
    expect(gateData.isOpen).toBe(false);
  });

  test('TCF-X09 Denied Exit - Plate Mismatch', async ({ request }) => {
    const userId = 1005;

    await request.get(`/api/simulation/scan-card?param=${userId}&read=true&isGuest=false`);
    const plateRes = await request.get(`/api/simulation/scan-plate?mode=correct&expectedPlate=MATCHPLATE`);
    const plateData = await plateRes.json();
    const licensePlate = plateData.licensePlate;

    await request.post(`/api/simulation/entrance`);

    const exitRes = await request.post(`/api/simulation/exit?userId=${userId}&licensePlate=WRONGPLATE`);
    expect(exitRes.status()).toBe(400);
    const exitData = await exitRes.json();
    expect(exitData.error).toContain('Exception: No active ticket found for this card (User ID: 1005)');

    const gateStatus = await request.get(`/api/simulation/gate-status`);
    const gateData = await gateStatus.json();
    expect(gateData.isOpen).toBe(false);
  });
});