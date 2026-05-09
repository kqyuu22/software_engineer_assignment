const { test, expect } = require('@playwright/test');


test.describe('IoT Management', () => {
  test.beforeEach(async ({ request }) => {
    await request.post('/api/simulation/reset-all');
  });

  test.afterEach(async ({ request }) => {
    await request.post('/api/simulation/reset-all');
  });

  test('TCF-I01 Sensor Reports Available/Occupied', async ({ request }) => {
    const slotId = 1;

    const beforeSlots = await request.get(`/api/simulation/slots`);
    const beforeData = await beforeSlots.json();
    const slotBefore = beforeData.find(s => s.slotId === slotId);
    const statusBefore = slotBefore.status;

    if (statusBefore === 'AVAILABLE') {
      await request.post(`/api/simulation/car-arrival?slotId=${slotId}`);
    } else {
      await request.post(`/api/simulation/car-departure?slotId=${slotId}`);
    }

    const afterSlots = await request.get(`/api/simulation/slots`);
    const afterData = await afterSlots.json();
    const slotAfter = afterData.find(s => s.slotId === slotId);
    expect(slotAfter.status).not.toBe(statusBefore);

    const occupiedBefore = beforeData.filter(s => s.status === 'OCCUPIED').length;
    const occupiedAfter = afterData.filter(s => s.status === 'OCCUPIED').length;
    expect(occupiedAfter).toBe(statusBefore === 'AVAILABLE' ? occupiedBefore + 1 : occupiedBefore - 1);
  });

  test('TCF-I02 Sensor Reports Failure', async ({ request }) => {
    const slotIds = [1, 2, 3, 4, 5, 6, 7];
    await request.post(`/api/simulation/sensor-failure-bulk`, { data: { slots: slotIds.join(',') } });

    const slots = await request.get(`/api/simulation/slots`);
    const slotData = await slots.json();
    const unknownCount = slotData.filter(s => s.status === 'UNKNOWN').length;
    expect(unknownCount).toBe(7);
  });

  test('TCF-I07 Mode - Sensor Failure Reaches Threshold', async ({ request }) => {
    await request.post(`/api/simulation/sensor-fix-bulk`, { data: { slots: "1-60" } });
    
    const slots = await request.get(`/api/simulation/slots`);
    const slotData = await slots.json();
    const availableSlots = slotData.filter(s => s.status === 'AVAILABLE').slice(0, 60).map(s => s.slotId);
    await request.post(`/api/simulation/sensor-failure-bulk`, { data: { slots: availableSlots.join(',') } });
    
    await new Promise(resolve => setTimeout(resolve, 6000));

    const modeRes = await request.get(`/api/simulation/system-mode`);
    const modeData = await modeRes.json();
    expect(modeData.mode).toBe('MONITOR');
  });

  test('TCF-I09 Directional guidance', async ({ request }) => {
    const signsRes = await request.get(`/api/simulation/signs`);
    expect(signsRes.ok()).toBeTruthy();
    const signsData = await signsRes.json();
    expect(signsData).toBeDefined();
  });

  test('TCF-I11 Status Broadcast', async ({ request }) => {
    const lotStatus = await request.get(`/api/simulation/lot-status`);
    const lotData = await lotStatus.json();
    expect(lotData.lotStatus).toBeDefined();
  });

  test('TCF-I13 Assign - Priority Slot Available', async ({ request }) => {
    const assignRes = await request.post(`/api/simulation/assign-spot?role=LECTURER`);
    expect(assignRes.ok()).toBeTruthy();
    const assignData = await assignRes.json();
    expect(assignData.success).toBe(true);
    expect(assignData.slotId).toBeGreaterThan(0);

    const slots = await request.get(`/api/simulation/slots`);
    const slotData = await slots.json();
    const assignedSlot = slotData.find(s => s.slotId === assignData.slotId);
    expect(assignedSlot.status).toBe('RESERVED');
  });
});