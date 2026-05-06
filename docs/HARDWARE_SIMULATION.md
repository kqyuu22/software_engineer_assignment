# Hardware Simulation & IoT Services

This documentation covers the simulated hardware and the API endpoints that operate the parking system. In a real-world scenario, these endpoints would be hit by actual IoT devices (sensors, cameras, gates) instead of user interfaces.

## IoT Services Overview

The Java application (`src/main/java/com/se/sebtl/service/iot/`) models the following hardware components:

- **Camera**: Simulates an OCR camera that scans license plates.
- **CardReader**: Simulates RFID card identification scans (Student/Staff IDs).
- **Gate**: Represents physical entry and exit barriers.
- **Sensor**: Individual mock hardware sensors mounted on each parking spot (300 in total). They report raw status changes directly to the IoT Manager.
- **IntersectionSign**: Digital signs placed at intersections (3 total). They route vehicles to specific sections based on their assigned spot.
- **HardwareSimulatorService**: Coordinates interactions with these mock hardware devices. It initializes sections on startup and runs environmental simulations.

> **Chaos Logic**: During the `car-arrival` simulation, there is a built-in **10% chance** a driver acts "rogue" and ignores their assigned spot, parking in a random available spot within the same section instead. The system naturally handles this via hardware sensor interrupts and auto-updates.

> However, in Simulation Page, we can trigger the chaos logic by choosing `car-arrival` in a slot different from the assigned one

---
## API Endpoint Simulation

### System Mode
Default is `NORMAL`. The system mode will change into `MONITOR` if:
- One of the sign has failure
- More than 25% of the sensors fails, meaning the status of 25% of the slots are `UNKNOWN`

When in `MONITOR` mode:
- Spot Assigned: Always 0
- Sign Directions: All `NONE`
- Parking Lot Status: `NULL`

### Hardware Status Manipulation
Operators and simulation tools can manually inject failures to test system resilience and automated mode switching.

**1. Sensor Failure Injection**
- Single Sensor: `POST /api/simulation/sensor-failure?slotId=${id}`
- Bulk Sensors: `POST /api/simulation/sensor-failure-bulk` (JSON body array of slot IDs)
- Fix Sensor: `POST /api/simulation/sensor-fix?slotId=${id}` or `sensor-fix-bulk`
*Triggers `MONITOR` mode if >25% of slots become `UNKNOWN`.*

**2. Sign Failure Injection**
- Retrieve signs: `GET /api/simulation/sign-failure`
- Toggle failure: `POST /api/simulation/sign-failure?signIndex=${index}&failed=${boolean}`
*Directly triggers `MONITOR` mode if any sign is marked as failed.*

### At the entrance
**1. Scan Card by Card Reader**
- Select Option in **Card Reader Status**
    - **success** or **fail**
    - **University Member** or **Guest**.

Note: When in **Guest** mode, ignore the card input.

- `GET /api/simulation/scan-card?param=${userId}&read=${cardReaderStatus === 'success'}`
    

**2. Scan License Plate by Camera**
- Select Option in **Camera Status**
    - **correct**: Return an exact license plate as the user input
    - **wrong**: Return a different license plate (randomly generated) from the user input. Note that this is still considered a *success scan*, the error is usually detected on screen showing the scanned, wrong logic check when query the database.
    - **fail**: Return null
    - **random**: Return a randomly generated license plate
- `GET /api/simulation/scan-plate?mode=${mode}&expectedPlate=${encodeURIComponent(expectedPlate)}`

**3. Entrance Simulation**
- Only continue if Camera and CardReader successfully. In other words, if we haven't had successful scans for both card ID and license plate, then we just send error in the frontend view. (I don't think this is an alert)

- Here is where we check for:
    - Non-existing SSO user ID &rarr; Alert `SECURITY_BREACH`
    - Duplicate SSO user ID &rarr; Alert `SECURITY_BREACH`. Note that Guest ID is generated automatically, so there won't be duplicate or non-existing Guest ID when checking at the entrance.
    - Duplicate Plate &rarr; Alert `SECURITY_BREACH`

- If no alerts or errors occur, we then add a new `ticket` into the database.

- Set the Gate status into *Open*.


### After the entrance
**1. Car Arrival:** Simulating the process when the customer arriving from the entrance to their parking slot.

- The assigned slot will be set to `RESERVED` and wait for a set of time. If timeout occurs, then the assigned slot will be set back to `AVAILABLE` and delete the `ticket` we've just created at the entrance.

- Trigger car arrival by route `POST /api/simulation/car-arrival?slotId=${id}`, then the slot status will be set to `OCCUPIED`

**2. Car Departure (Hardware Trigger):** Simulating the car vacating the physical parking slot.
- Fetch `POST /api/simulation/car-departure?slotId=${id}`.
- The API Endpoint will send the frontend information about the ticket linked to the slot ID:
    - License plate
    - Ticket ID
    - Ticket Type: `GUEST` or `SSO`
    - Entry Time
    - Exit Time
    - User ID: named Holder Identifier in TicketView, is a 4-digit number for `SSO`, and `GUEST-<ID>` for `GUEST` in TicketView
    - Price: Extract from table price, which is configured based on role (LECTURER, STUDENT, STAFF, OTHER)
- The hardware sensor directly tells the IoT Manager the space is now empty, updating the slot status to `AVAILABLE`.
- The UI will show the ticket information (just listed above) along with a Pay button. About the Pay button:
    - If ticket type is `GUEST`
        - Open a modal which has 2 buttons: "Pay by Cash" and "Pay by QR Code"
        - If "Pay by Cash" is clicked => Considered Paid
        - If "Pay by QR Code" is clicked => Show a QR code based on a certain information. Decide on a QR Data and create a QR code based on that data, i.e. the code below
        ```
        const qrData = `Pay Ticket, UID: ${userId}, Amount: $${price}, Receiver: Parking Lot, Ticket Type: ${ticketType}`;

        const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(qrData)}`;
        ```
        - Click "I have scanned and pay", in real life, we will validate via the banking service. => Finish Payment

    - If ticket type is `SSO`:
        - Just click the Pay button, the backend will handle sending the payment to the correct table in the database. In real life, this will be send to the BKPay service. Therefore, we will show a note: "We already sent your ticket payment to BKPay"

- After the payment is finished, the User Id and License Plate will be shown on the exit inputs, ready for you to click exit with this info.

> **NOTE**: Currently, if you trigger car departure without exitting, we will have a "hanging ticket" where its parking slot is marked as `AVAILABLE` but the ticket is not finished, meaning some new ticket may be assigned to this parking slot - who is still linked with another unfinished ticket.


**3. Car Exit (Kiosk Resolution):** Simulating the car arriving at the exit gate, scanning its details, and leaving the premises.
- Fetch `POST /api/simulation/exit?userId=${uid}&licensePlate=${encodeURIComponent(plate)}`
- This validates the user:
    - If the user ID is unfound &rarr; Alert `SECURITY_BREACH`
    - If the license plate is not matched with the user ID &rarr; Alert `SECURITY_BREACH`



- Update the ticket's exit time, marks the ticket as finished, and safely opens the exit Gate.

## Note About Simulation Page
- Auto refresh every `AUTO_REFRESH_INTERVAL` and each refresh, fetch:
    - Slots
    - Sign Directions
    - Gate Status (OPEN/CLOSE)
    - Parking Lot Status (NEARLY_FULL/FULL/AVAILABLE)
    - System Mode (NORMAL/MONITOR)
    - Sign Failures
- **Manual Overrides**: Operators can manually inject sensor faults, fix sensors, or update a bulk of sensors simultaneously (e.g. `POST /api/simulation/sensor-failure-bulk`) to evaluate system resilience.
- **Chaos Testing**: Dropdowns allow forcing specific device behaviors during entry, including camera OCR misreads (`wrong`, `fail`) and card reader failures to thoroughly test edge cases in entrance logic. 
