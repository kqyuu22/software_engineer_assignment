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