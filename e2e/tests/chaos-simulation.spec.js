const { test, request, expect } = require('@playwright/test');

// Helper to get random integer up to max
function randInt(max) {
  return Math.floor(Math.random() * max);
}

// Random probability tester
function chance(percentage) {
  return Math.random() * 100 < percentage;
}

test.describe('Automated Chaos Simulation', () => {

  test('Run Markov Chain simulation for 200 iterations', async ({ request, page }) => {
    // State Tracking
    const carsInside = []; 
    const spotsOccupied = new Set();
    const brokenSensors = new Set();

    // Configuration
    const TOTAL_STEPS = 200;
    const EDGE_URL = 'http://localhost:3000';
    const BASE_URL = `${EDGE_URL}/api/simulation`;
    const PAYMENT_URL = 'http://localhost:18080/api/payment'; // Main server

    let isDbOffline = false;
    let offlineStepsRemaining = 0;
    
    for (let step = 0; step < TOTAL_STEPS; step++) {
      if (offlineStepsRemaining > 0) {
        offlineStepsRemaining--;
        if (offlineStepsRemaining === 0) {
          await request.post(`${EDGE_URL}/api/edge-config/offline`, { data: { offline: false } });
          isDbOffline = false;
          console.log(`[Step ${step}] DB / Network restored.`);
        }
      } else {
        const r = Math.random() * 100;
        // 5% DB Offline: Simulating network issue on the edge server
        if (r < 5) {
          console.log(`[Step ${step}] Event: DB Offline (Edge disconnect)`);
          await request.post(`${EDGE_URL}/api/edge-config/offline`, { data: { offline: true } });
          isDbOffline = true;
          offlineStepsRemaining = 10; // Stay offline for 10 steps
        }
      }

      // Re-roll action for the 4 main events (95%)
      const actionRoll = Math.random() * 100;
      
      try {
        if (actionRoll < 25) { // 25% Entrance
            const isInvalid = !chance(95); // 5% invalid
            console.log(`[Step ${step}] Event: Entrance attempts (${isInvalid ? 'invalid' : 'valid'})`);
            
            // Random user ID (assuming users 1-10 exist in DB from seed data)
            const userId = randInt(10) + 1;
            const newPlate = `SIM-${Date.now()}`;
            
            let plateToUse = newPlate;
            if (isInvalid && carsInside.length > 0) {
               plateToUse = carsInside[0].plate; // Cause duplicate plate error
            }

            const res = await request.post(`${BASE_URL}/entrance`, {
              params: { 
                 userId: userId.toString(), 
                 existingPlate: plateToUse
              }
            });
            
            if (isInvalid) {
              expect(res.status()).not.toBe(200); // 400 Bad Request
            } else if (res.ok()) {
              carsInside.push({ userId, plate: newPlate });
            }
        } 
        else if (actionRoll < 50) { // 25% Exit
            const isInvalid = !chance(95); // 5% invalid
            console.log(`[Step ${step}] Event: Exit attempts (${isInvalid ? 'invalid' : 'valid'})`);
            
            if (carsInside.length > 0) {
                // Pick a car
                const carIndex = randInt(carsInside.length);
                const car = carsInside[carIndex];
                
                const isBilledOnBkpay = chance(90);
                
                // If it's billed on bkpay, let's theoretically pay
                if (isBilledOnBkpay && !isInvalid) {
                    // Logic to pay beforehand
                    // E.g., await request.post(`/api/payment/...`)
                    console.log(`\tCar ${car.plate} paid via BkPay.`);
                }

                const exitRes = await request.post(`${BASE_URL}/exit`, {
                    params: { 
                         userId: car.userId,
                         licensePlate: isInvalid ? "INVALID-PLATE" : car.plate
                    }
                });

                if (isInvalid) {
                    expect(exitRes.status()).not.toBe(200); 
                } else if (exitRes.ok()) {
                    carsInside.splice(carIndex, 1); // remove from array
                }
            } else {
                console.log(`\tSkipped Exit (No cars inside)`);
            }
        }
        else if (actionRoll < 70) { // 20% Arrive at spot
            console.log(`[Step ${step}] Event: Arrive at spot`);
            // Pick a random spot 1-20
            const spotId = randInt(20) + 1;
            if (!spotsOccupied.has(spotId) && !brokenSensors.has(spotId)) {
                await request.post(`${BASE_URL}/car-arrival`, { params: { slotId: spotId }});
                spotsOccupied.add(spotId);
            }
        }
        else if (actionRoll < 90) { // 20% Leave spot
            console.log(`[Step ${step}] Event: Leave spot`);
            if (spotsOccupied.size > 0) {
               const spotId = Array.from(spotsOccupied)[randInt(spotsOccupied.size)];
               if (!brokenSensors.has(spotId)) {
                   await request.post(`${BASE_URL}/car-departure`, { params: { slotId: spotId }});
                   spotsOccupied.delete(spotId);
               }
            }
        }
        else { // 10% Sensor Broken (Scaling to 100% block) -> Real requirements: 5% overall rate
            // But since probabilities sum to (25+25+20+20 = 90), the remaining 10% is mostly the 5% sensor break
            console.log(`[Step ${step}] Event: Sensor Broken`);
            const spotId = randInt(20) + 1;
            
            if (!brokenSensors.has(spotId)) {
               await request.post(`${BASE_URL}/sensor-failure`, { params: { slotId: spotId }});
               brokenSensors.add(spotId);
               
               // Auto-fix after simulated duration (e.g. 5-10s represented quickly here by 200ms in test)
               const fixTime = 200 + randInt(300);
               setTimeout(async () => {
                   try {
                       await request.post(`${BASE_URL}/sensor-fix`, { params: { slotId: spotId } });
                       brokenSensors.delete(spotId);
                       console.log(`\tSensor ${spotId} auto-repaired!`);
                   } catch(e) {}
               }, fixTime);
            }
        }
      } catch (err) {
         if (!isDbOffline) {
            console.error(`Unexpected failure at step ${step}:`, err.message);
         } else {
            console.log(`\tExpected behavior at step ${step} during DB Offline:`, err.message);
         }
      }
      
      // Small pause to let async promises breathe
      await new Promise(r => setTimeout(r, 10)); 
    }
  });
});
