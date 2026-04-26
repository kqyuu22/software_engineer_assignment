## Run the app


1. find src/main/java/com/se/sebtl/SebtlApplication.java and click Run
2. mvn spring-boot:run on command line

-> Access http://localhost:8080
-> It automatically redirect you to http://localhost:8080/login and serve `auth.html`
-> Currently Support: /member, /operator, /admin
-> Still Developing: a page for simulation 

## Currently: Only cover a demo for
```
OperatorController.java
ParkingSlot.java
ParkingSlotRepository.java
operator.html
```
## What to do ?
**Frontend**: `operator.html` is a mockup frontend, later we can use React to handle the same logic (read the route and return json in the controller). Note that you can add a mockup frontend just like this, and implement React later.

**Controller**: `AdminController.java`, `LoginController.java`, `MemberController.java`. Note that you can add more database query logic in repository/, add more repositories and tables in repository/ and model/, but don't remove anything.

**Database and Backend:** There are some missing logic regarding
- Alert
- Payment
- Entry Simulation (such as scan card, scan license plate, etc)
- Local Queue
- Maybe more if i remember

## How to run the Simulation

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

## Return format for controllers
Each controller will return a JSON file