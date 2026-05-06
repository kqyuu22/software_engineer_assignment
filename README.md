## Run the app

1. find src/main/java/com/se/sebtl/SebtlApplication.java and click Run
2. mvn spring-boot:run on command line

&rarr; Access http://localhost:18080

&rarr; It automatically redirect you to http://localhost:18080/login and serve `auth.html`

&rarr; Currently Support: /member, /operator, /admin, /simulation

## Simulation Flow
**READ THIS IF YOU WANT TO TEST `/simulation`**

During testing `/simulation`, see this section for clearer details. The numbering is strictly meaningful in this section.

### **Normal Flow**
*Do as these steps:*

**1. Set the Hardware Status**
- System Mode: *NORMAL*
- Camera Status: *random* or *correct* or *wrong*
- Card Reader Status: *Success*, *Guest* or *University Member* are both fine.
- Gate Status: doesn't really matter
- Signs: All *normal* (default)

**2. Click Scan ID and Scan Plate**
- *Guest*: Don't care Input User ID
- *University Member*: Must input an ID
- Camera Status:
    - *random*: Don't care what plate is inputted, easy for testing
    - *success*: Will read exactly what plate you input
    - *wrong*: Will read a different plate from what you input, still considered a successful scan
**Note**: We do not have any features to address the wrongly scanned license plate, we still treat it as a success scan at the entrance.

**3. Click Execute Sequence Entrance**
**Note**: You have to both click Scan ID and Scan Plate successfully.

***WAIT FOR A BIT***

Because the backend handles this quite slow, and only when the backend finishes, then the frontend can fetch the data and show.

When success, you will receive an **assigned spot** - which is automatically shown **beside the Trigger Arrival button**.

**4. Click Trigger Arrival button**
If you wait for a bit *too* long, the timeout in the backend triggers, and Trigger Arrival is meaningless (you have to execute the entrance sequence again). Yes, this is such a pain. We currently do not have UI to show the timeout.

You can still see the `RESERVED` slot status in the UI. If the slot status is back to `AVAILABLE` later on, this means Trigger Arrival failed.

If you Trigger Arrival succeeds, then the slot status is changed to `OCCUPIED`. The **Ticket Section** will show the new ticket too.


> This step finishes the full sequence from the entrance gate to the parking slot.

**5. Choose an OCCUPIED slot ID and click Depart button**
- The ticket information linked to that slot ID will be shown in **Ticket Information of the Slot Departure**. There will also appear a **Pay Button**.

**6. Click Pay button**
- For `SSO` ticket type: That's it! The backend will automatically create a billing id linked to the SSO ticket.
- For `GUEST` ticket type: A modal appears! Choose Pay by Cash or Pay by QR Code, either way, the guest ticket will be automatically updated `final_calculated_fee` and `paid_directly`.

If payment success, then the User ID and License Plate of that ticket payment will be automatically appear in the Exit Section - right above **Settle Ticket & Exit** button.

**7. IMPORTANT: Remember to Click Settle Ticket & Exit button RIGHT AFTER PAYMENT**
If you don't, we will have a "hanging ticket". Yes, this is also a pain, but we have not resolved this.

That's it. Finish.

## Still Developing... 
- Simulation UI and Simulation Loigc:
    - There are classes for initial configuration, but no parameters to input config and thus, no UI
- Automated test cases (the one using playwright JS) are still very basic



<!-- 
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
7. **Parking Lot Status:** Monitors occupancy to set lot status as `AVAILABLE`, `NEARLY_FULL`, or `FULL`. -->

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
