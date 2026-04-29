## Run the app

1. find src/main/java/com/se/sebtl/SebtlApplication.java and click Run
2. mvn spring-boot:run on command line

&rarr; Access http://localhost:18080

&rarr; It automatically redirect you to http://localhost:18080/login and serve `auth.html`

&rarr; Currently Support: /member, /operator, /admin

&rarr; Still Developing: a page for simulation

## Currently Lacking...
**Frontend**: ***READ `FRONTEND.md`***. This currently covers mockup frontend, later we can use React to handle the same logic (read the route and return json in the controller). Note that you can add a mockup frontend just like this, and implement React later.

**Simulation**: We currently lack Entry and Exit Simulation, and also Controller for their UIs.

## How to run the current Simulation

The application includes a command-line simulation (`SimulationRunner.java`) that starts automatically when you run the Spring Boot app. It provides an interactive CLI menu to test the parking system hardware interactions and system logic without the need for physical sensors or frontend applications.

**Running the simulation:**
Running the application via IDE (Navigate to `SebtlApplication.java` and click **Run**) or command line (`mvn spring-boot:run`) will present a menu in the terminal output. Follow the on-screen prompts to input numbers to interact with the system.

**What the simulation offers:**
The simulation allows you to test:
1. **Spot Assignment:** Simulates an entry kiosk requesting a spot for different user roles (STAFF, LECTURER, OTHER). It reserves the spot and starts a 2-minute timer. If the car doesn't arrive in time, the spot is released.
2. **Car Arrival (Hardware Sensor):** Simulates a car arriving at an assigned spot, triggering the occupied state. Includes a *chaos factor* where there is a 10% chance a driver parks in a random available spot in their section instead of the assigned one.
3. **Car Departure (Hardware Sensor):** Simulates a car leaving a spot, freeing it up in the system.
4. **Sensor Failure:** Simulates hardware malfunction, putting the spot in an `UNKNOWN` state.
5. **Fix Sensor:** Simulates an operator or technician fixing a broken sensor, restoring its functionality and resetting its state.
6. **Dynamic Sign Routing:** When a spot is assigned, the system dynamically updates virtual intersection signs to route the driver.
7. **Parking Lot Status:** Monitors occupancy to set lot status as `AVAILABLE`, `NEARLY_FULL`, or `FULL`.

## Automated testing
Added testing for basic functionalities, placed in the `e2e/tests` folder:

### 1. `member.spec.js`
-   Logs in as a `MEMBER`.
-   Navigates to `/member.html`.
-   Verifies that the Parking History and Payment tables are visible and loaded correctly.

### 2. `operator.spec.js`
-   Logs in as an `OPERATOR`.
-   Verifies that the Active Alerts, Resolved Alerts, Real-Time Slots, and Ticket History components render correctly on load.
-   Types "GUE" into the search bar, submits it, waits for the `history/search` API to return, and verifies the table updates.
-   Clears the search.
-   Searches for the "Resolve" button for active alerts. If it exists, it clicks it, automatically accepts the popup dialog, and verifies the success response.

### 3. `admin.spec.js`
-   Logs in as an `ADMIN`.
-   Types `6000` into the new price input and submits it, checking if the UI updates successfully.
-   Automatically handles the browser alerts/toasts.
-   Selects a priority from the dropdown and successfully creates a new Slot.
-   Inputs a range (e.g., `1-2`) and changes multiple slot priorities simultaneously using the bulk update functionality.
-   Verifies that searching for a ticket using the admin interface works identically to the operator interface.

### How to run automated testing
To run all of these tests, simply navigate into the `e2e` folder, install the dependencies, and run them:

```bash
cd e2e
npm install
npx playwright install --with-deps chromium
npm test
```

Playwright will automatically start your Spring Boot API in the background, run all three test specs concurrently in headless Chrome browsers, and tell you if any features break!

For chaos simulation, run this instead:
```bash
npx playwright test tests/chaos-simulation.spec.js
```
(Note: chaos simulation is partial, still need to be improved.) 