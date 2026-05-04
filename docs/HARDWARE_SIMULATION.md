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

---

## REST Endpoints (`/api/simulation`)
The Simulation Controller (`SimulationApiController.java`) allows the frontend or testers to trigger these hardware events manually via HTTP.

### 1. Hardware State Verification
*   `GET /api/simulation/slots`: Gets the real-time status of all parking slots.
*   `GET /api/simulation/signs`: Gets the current digital directional arrows on all Intersection Signs to verify correct routing paths.

### 2. Entrance Process (Kiosk Simulator)
*   `GET /api/simulation/scan-plate`: Mocks capturing a random license plate using the simulated `Camera`. Returns `{ "licensePlate": "XX-XXXX" }`.
*   `POST /api/simulation/assign-spot?role={role}`: Mocks a kiosk assigning a spot based on the driver's role. Changes the spot state to `RESERVED` and starts a 2-minute timer. If full, triggers a system alert.
*   `POST /api/simulation/entrance?userId={userId}&existingPlate={plate}`: Completes the entrance process, tying the license plate and user ID, saving the ticket, and opening the `Gate`.

### 3. Exit Process
*   `GET /api/simulation/active-tickets`: Quickly retrieves active (unfinished) tickets so testers can simulate the correct car attempting an exit.

### 4. Sensor Hardware Triggers (Spot Level)
These mimic the individual spot sensors detecting a car's physical presence or absence.
*   `POST /api/simulation/car-arrival?slotId={id}`: Simulates a car physically driving into a spot. Triggers the sensor to switch to `OCCUPIED`. (This is where Chaos Logic triggers).
*   `POST /api/simulation/car-departure?slotId={id}`: Simulates a car leaving a spot. Triggers the sensor to switch back to `AVAILABLE`.

### 5. Hardware Maintenance Simulation
*   `POST /api/simulation/sensor-failure?slotId={id}`: Injects a mock hardware fault, marking the spot state as `UNKNOWN` and reporting a **SYSTEM_FAILURE** to the Alert dashboard.
*   `POST /api/simulation/sensor-fix?slotId={id}`: Simulates an engineer fixing the sensor. It restores the slot to its previous known state.

---

## Frontend Control Panel (`simulation.html`)

The frontend component (`src/main/resources/static/simulation.html`) provides a dashboard map and control board for testing system rules without needing a physical setup.

**Key Testing Areas:**
- **Live Hardware Map:** Visualizes spots across Sections A (1-100), B (101-200), and C (201-250+), color-coded by state (`AVAILABLE` = Green, `OCCUPIED` = Red, `RESERVED` = Orange, `UNKNOWN` = Grey).
- **Signage Logic Flow:** Displays real-time changes to routing signs when you generate a ticket for a user.
- **Entrance & Exit Simulation:** Step-by-step testers for the entire flow from ID scan to Checkout, ensuring tickets and sensors link correctly.
- **Overrides & Maintenance:** Allows forcing spots into failure/repair states, or forcing arbitrary arrivals to test operator dashboards and alert generation.