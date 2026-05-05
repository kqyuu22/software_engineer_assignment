# Testcase Implementation Plan

## 1) Scope and intent
This plan maps every test case in `Testcase.md` to concrete automated Playwright checks and/or API-level checks, grouped by section. It also calls out backend gaps where required behaviors are not currently implemented, so those tests will be marked as blocked (or `test.skip`) until hooks exist.

## 2) Codebase map (relevant surfaces)
- Frontend pages: `src/main/resources/static/auth.html`, `src/main/resources/static/member.html`, `src/main/resources/static/operator.html`, `src/main/resources/static/admin.html`, `src/main/resources/static/simulation.html`.
- Auth/session: `src/main/resources/static/auth.js`.
- Shared UI helpers: `src/main/resources/static/shared.js`.
- API controllers: `src/main/java/com/se/sebtl/controller/*`.
- IoT services: `src/main/java/com/se/sebtl/service/IoTManagerService.java`, `src/main/java/com/se/sebtl/service/iot/*`.
- Seed data: `database/03_seed_data.sql`.
- Playwright: `e2e/playwright.config.js`, `e2e/tests/*.spec.js`.

## 3) Test organization
Create new test specs by area:
- `e2e/tests/entry-exit.spec.js` (TCF-E*, TCF-X*)
- `e2e/tests/iot.spec.js` (TCF-I*)
- `e2e/tests/app-auth.spec.js` (TCF-L01-L04)
- `e2e/tests/app-member.spec.js` (TCF-L05-L07)
- `e2e/tests/app-operator.spec.js` (TCF-L08-L12, alerts)
- `e2e/tests/app-admin.spec.js` (TCF-L13-L15)
- `e2e/tests/nonfunctional.spec.js` (TCNF-*)

Shared helpers in `e2e/tests/helpers/`:
- `api.js`: login() -> token, entrance(), exit(), assignSpot(), carArrival(), carDeparture(), sensorFailure(), getSlots(), getSigns(), getActiveTickets(), getAlerts(), resolveAlert().
- `ui.js`: loginAs(role), logout(), expectRedirectTo(role), waitForToastOrAlert().

## 4) Test data and setup
- Use seeded users from `database/03_seed_data.sql`:
  - member: `member01` (1001 / pass `123`)
  - operator: `operator01` (1003 / pass `password123`)
  - admin: `admin01` (1005 / pass `password123`)
- Prefer deterministic plates by passing `existingPlate` into `/api/simulation/entrance`.
- Use `test.describe.serial` for stateful flows (reservation timers, full lot scenarios).

## 5) Functional tests - Entry (TCF-E01..E10)
### TCF-E01 Successful Entry - University Member
- API: `POST /api/simulation/entrance?userId=1001&existingPlate=55AA11111`
- Assert 200, response contains `ticketId`, `slotId`, `licensePlate`.
- Verify slot is RESERVED via `GET /api/simulation/slots`.

### TCF-E02 Successful Entry - Visitor
- API: `POST /api/simulation/entrance?userId=0&existingPlate=55AA22222`
- Assert 200, ticket created (guest), gate open implied by success.

### TCF-E03 DATACORE Unreachable
- Gap: no DATACORE service/flag. Current behavior for unknown user is guest.
- Mark as partial (assert guest handling for unknown user).

### TCF-E04 DB Unreachable
- Gap: no DB failure injection.
- Mark blocked.

### TCF-E05 Gate Hardware Failure
- Gap: no gate failure mode.
- Mark blocked.

### TCF-E06 Denied - No Spot Available
- Requires all slots occupied/reserved.
- Implement as serial long test: reserve or occupy every slot, then call entrance, expect 400.
- If too slow, mark manual.

### TCF-E07 Denied - ID Not Found
- Gap: current code treats as guest, not deny.
- Mark blocked (backend mismatch).

### TCF-E08 Denied - Duplicate Session
- Gap: no duplicate user check in entrance.
- Mark blocked.

### TCF-E09 Denied - Plate Undetectable
- Gap: camera failure not used in entrance path (always returns plate).
- Mark blocked.

### TCF-E10 Denied - Duplicate Plate
- API: create ticket with plate X, then entrance with same plate X.
- Expect 400 and SECURITY_BREACH alert in `/operator/alerts/active`.

## 6) Functional tests - Exit (TCF-X01..X11)
### TCF-X01 Successful Exit - University Member
- Use existing active ticket (seeded user 1001 + plate `55BB34567`).
- API: `POST /api/simulation/exit?userId=1001&licensePlate=55BB34567`
- Assert 200, ticket closed, slot freed.

### TCF-X02/X03 Visitor via QR/Cash
- Gap: payment processing not implemented in simulation.
- Mark blocked.

### TCF-X04 DB Unreachable
- Gap: no DB failure injection.
- Mark blocked.

### TCF-X05 BKPay Unreachable
- Gap: no BKPay integration.
- Mark blocked.

### TCF-X06 Gate Hardware Failure
- Gap: no gate failure mode.
- Mark blocked.

### TCF-X07 Denied - No Active Ticket
- API: exit with userId 1002 (no active ticket), any plate.
- Expect 400 + SECURITY_BREACH alert.

### TCF-X08 Denied - Plate Undetectable
- Gap: no camera capture in exit endpoint.
- Mark blocked.

### TCF-X09 Denied - Plate Mismatch
- API: exit with userId 1001 but wrong plate.
- Expect 400 + SECURITY_BREACH alert.

### TCF-X10/X11 Visitor Payment Failure/Timeout
- Gap: no cash/QR failure handling.
- Mark blocked.

## 7) IoT Management - Group 1 (TCF-I01..I02)
### TCF-I01 Sensor Reports Available/Occupied
- `POST /api/simulation/car-arrival?slotId=5` -> slot OCCUPIED.
- `POST /api/simulation/car-departure?slotId=5` -> slot AVAILABLE.

### TCF-I02 Sensor Reports Failure
- `POST /api/simulation/sensor-failure?slotId=6` -> slot UNKNOWN.
- Gap: failure counts/system mode not implemented.

## 8) IoT Management - Group 2 (TCF-I03..I08)
### TCF-I03/I04/I05 Lot Status Thresholds
- Occupy slots to reach occupancy <0.9, =0.9, =1.0.
- Poll `GET /api/simulation/signs` -> `lotStatus` should be AVAILABLE/NEARLY_FULL/FULL.

### TCF-I06/I07 Sensor Failure Thresholds
- Gap: no failure counters or SystemMode.
- Mark blocked.

### TCF-I08 DB Unreachable
- Gap: no DB failure injection.
- Mark blocked.

## 9) IoT Management - Group 3 (TCF-I09..I12)
### TCF-I09 Directional Guidance
- `POST /api/simulation/assign-spot?role=STAFF`.
- Check `GET /api/simulation/signs` for expected sign directions based on slot range.

### TCF-I10 Directional Update
- Same as I09, verify signs update after assignment.

### TCF-I11 Status Broadcast
- Use occupancy threshold changes, verify `lotStatus` changes.

### TCF-I12 Sign Unreachable
- Gap: no sign failure reporting.
- Mark blocked.

## 10) IoT Management - Group 4 (TCF-I13..I18)
### TCF-I13 Priority Slot Available
- Assign spot for STAFF/LECTURER/STUDENT, assert lowest ID in that priority band.

### TCF-I14 Priority Slot Exhausted
- Reserve all priority slots for a role, then assign again, expect fallback to lowest available.

### TCF-I15 No Slots Available
- Full lot required, same as E06.

### TCF-I16 Reservation Timer Started
- Call `assign-spot`, verify slot RESERVED, wait <30s still RESERVED.

### TCF-I17 Reservation Timeout
- After 30s, slot returns to AVAILABLE if no arrival.

### TCF-I18 Sensor Confirms Before Timeout
- Assign spot, immediately `car-arrival`, wait >30s, slot remains OCCUPIED.

## 11) Application - Group 1 Login and Session (TCF-L01..L04)
### TCF-L01 Login
- UI login for each role, assert redirect and page heading.

### TCF-L02 SSO/DATACORE Unreachable
- Gap: no outage simulation. Use invalid credentials as partial.

### TCF-L03 Logout Manual
- Click injected Logout, assert redirect `/login` and session cleared.

### TCF-L04 Session Timeout
- Gap: no expiration logic. Use manual session clear as partial.

## 12) Application - Group 2 Functions (TCF-L05..L15)
### TCF-L05 Member History ordering
- Check active ticket listed first, then completed by entry time.

### TCF-L06 Pay Bills Redirect
- Gap: no BKPay redirect. Current implementation is POST then status update.

### TCF-L07 Logout Manual
- Same as L03.

### TCF-L08 Operator Real-Time View
- UI table renders, matches `/operator/slots`.

### TCF-L09/L10 Alert Prioritization + Resolve
- Trigger SECURITY_BREACH (duplicate plate). Verify ordering in active alerts (security before system failure). Resolve an alert and verify removal/history.

### TCF-L11/L12 Manage History
- Default order newest->oldest, search filters by query.

### TCF-L13 Adjust Price Success
- Admin updates price, assert UI and `/admin/price` reflect change.

### TCF-L14 Adjust Price Invalid
- Gap: no validation for zero/negative in UI or backend. Mark blocked.

### TCF-L15 Priority Bulk Update
- Admin bulk update, verify slot priorities changed via `/admin/slots`.

## 13) Non-functional (TCNF-01..TCNF-13)
- TCNF-01/02/03/04/05/06/07: Use Playwright timing (`performance.now()`) on page load and operation completion, keep thresholds soft to avoid flakiness.
- TCNF-08 Load: not suitable for Playwright, recommend k6/JMeter as separate performance plan.
- TCNF-09/10/11: Role-based access via API (expect 403) plus UI session role tampering (expect redirect).
- TCNF-12: Alert ordering from `/operator/alerts/active` strictly security before system failure.
- TCNF-13: Use token after logout, expect 401/403.

## 14) Blockers and backend hooks needed
- DB unreachable simulation.
- DATACORE/SSO outage simulation.
- Gate failure simulation.
- Camera failure path in entrance/exit.
- Payment logic (cash/QR/BKPay).
- System mode and sensor failure thresholds.
- Sign failure reporting.

## 15) Execution order
1) Build helpers and login utilities.
2) Implement app auth + member + operator + admin UI tests.
3) Implement entry/exit API tests (happy path + security breaches).
4) Implement IoT tests (sensor + routing + reservation).
5) Add non-functional tests with soft thresholds.
6) Add skipped tests for blocked items with explicit reasons.

## 16) Reporting
- Each skipped test references the missing backend capability.
- Each test maps to a TC ID in its title, for example `TCF-E01 Successful Entry - Member`.

## Skipped tests
- TCF-E04
- TCF-E05
- TCF-E06
- TCF-E07
- TCF-E08
- TCF-E09
- TCF-X02
- TCF-X03
- TCF-X04
- TCF-X05
- TCF-X06
- TCF-X08
- TCF-X10
- TCF-X11
- TCF-I06
- TCF-I07
- TCF-I08
- TCF-I12
- TCF-I15
- TCF-L02
- TCF-L04
- TCF-L06
- TCF-L14
- TCNF-08
