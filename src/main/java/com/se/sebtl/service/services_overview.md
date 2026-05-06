# Service Classes Overview

This document describes all the classes, their attributes, methods, and relationships within the `src/main/java/com/se/sebtl/service` directory.

## 1. IoTManagerService

The `IoTManagerService` handles IoT devices, tracking parking lot status, system mode, sensors, and managing parking slot reservations. 

*   **Location:** `com.se.sebtl.service.IoTManagerService`
*   **Dependencies:** `ParkingSlotRepository`, `TaskScheduler`, `TransactionTemplate`, `TicketRepository`, `SsoTicketRepository`, `GuestTicketRepository`.

### Attributes
*   `slotRepository` (ParkingSlotRepository)
*   `taskScheduler` (TaskScheduler)
*   `transactionTemplate` (TransactionTemplate)
*   `ticketRepository` (TicketRepository)
*   `ssoTicketRepository` (SsoTicketRepository)
*   `guestTicketRepository` (GuestTicketRepository)
*   `signs` (List<IntersectionSign>)
*   `currentSignDirections` (Map<Integer, Direction>)
*   `signFailures` (Map<Integer, Boolean>)
*   `currentLotStatus` (ParkingLotStatus)
*   `mode` (SystemMode)
*   `sensorFailure` (boolean)
*   `signFailure` (boolean)

### Methods
*   `IoTManagerService(ParkingSlotRepository, TaskScheduler, TransactionTemplate, TicketRepository, SsoTicketRepository, GuestTicketRepository)`: Constructor.
*   `setSigns(List<IntersectionSign> signs)`
*   `getSignDirections(): Map<String, Object>`
*   `assignSpot(Role role): int`
*   `updateSigns(int slotId)`
*   `getDirectionForSlot(int slotId, int signIndex): Direction`
*   `startReservationTimer(int slotId)`
*   `onSensorUpdate(int slotId, SlotStatus status)`
*   `updateParkingLotStatus()`

## 2. ParkingService

The `ParkingService` is responsible for querying and managing general parking data like slots, tickets, and pricing.

*   **Location:** `com.se.sebtl.service.ParkingService`
*   **Dependencies:** `TicketViewRepository`, `ParkingSlotRepository`, `PriceRepository`.

### Attributes
*   `ticketViewDb` (TicketViewRepository)
*   `slotDb` (ParkingSlotRepository)
*   `priceDb` (PriceRepository)

### Methods
*   `ParkingService(TicketViewRepository, ParkingSlotRepository, PriceRepository)`: Constructor.
*   `getAllTickets(): List<TicketView>`
*   `getAllSlots(): List<ParkingSlot>`
*   `getCurrentPrice(Role priority): java.math.BigDecimal`
*   `searchTickets(String query): List<TicketView>`

## 3. SecurityService

The `SecurityService` manages authentication and role verification using tokens.

*   **Location:** `com.se.sebtl.service.SecurityService`
*   **Dependencies:** `AppUserRepository`.

### Attributes
*   `userDb` (AppUserRepository)

### Methods
*   `SecurityService(AppUserRepository)`: Constructor.
*   `parseToken(String token): List<Object>`
*   `verifyRole(String token, AppRole expectedRole): AppUser`
*   `getUserIdFromToken(String token): int`
*   `validateExpiry(String token)`

## IoT Subpackage Classes (`service.iot`)

### 4. Camera
Simulates an IoT camera for capturing license plates.
*   **Location:** `com.se.sebtl.service.iot.Camera`
*   **Attributes:** `random` (Random), `lastCapturedPlate` (String)
*   **Methods:** `generatePlate(): String`, `capture(): String`, `captureCorrect(String expectedPlate): String`, `captureWrongPlate(): String`, `captureFail(): String`, `getLastCapturedPlate(): String`

### 5. CardReader
Simulates a card reader for scanning user ID cards.
*   **Location:** `com.se.sebtl.service.iot.CardReader`
*   **Attributes:** `simulatedUserId` (int), `isGuest` (boolean)
*   **Methods:** `CardReader()` (Constructor), `readCard(): int`, `setSimulatedUserId(int simulatedUserId)`, `isGuest(): boolean`

### 6. Gate
Simulates an IoT entry/exit gate with delayed closing logic.
*   **Location:** `com.se.sebtl.service.iot.Gate`
*   **Attributes:** `isOpen` (boolean)
*   **Methods:** `open()`, `close()`, `closeDelayed()`, `isOpen(): boolean`

### 7. HardwareSimulatorService
Orchestrates simulation of hardware devices (sensors, signs) and driver behaviors (e.g. parking in wrong spots).
*   **Location:** `com.se.sebtl.service.iot.HardwareSimulatorService`
*   **Dependencies:** `IoTManagerService`, `ParkingSlotRepository`.
*   **Attributes:** `iotManager` (IoTManagerService), `slotRepository` (ParkingSlotRepository), `sensors` (List<Sensor>), `signs` (List<IntersectionSign>), `random` (Random)
*   **Methods:** `HardwareSimulatorService(IoTManagerService, ParkingSlotRepository)` (Constructor), `initializeHardware()`, `simulateCarArrival(int assignedSlotId)`, `simulateCarDeparture(int slotId)`, `simulateSensorFailure(int slotId)`, `simulateSensorFix(int slotId)`

### 8. IntersectionSign
Simulates an IoT directional sign within the parking lot.
*   **Location:** `com.se.sebtl.service.iot.IntersectionSign`
*   **Attributes:** `iotManager` (IoTManagerService)
*   **Methods:** `IntersectionSign(IoTManagerService)` (Constructor), `updateDirection(Direction direction)`, `updateStatus(ParkingLotStatus status)`, `sendFail()`

### 9. PaymentSlot
Simulates an IoT payment machine processing cash or QR code transactions.
*   **Location:** `com.se.sebtl.service.iot.PaymentSlot`
*   **Attributes:** `cashAvailable` (boolean), `bankingAvailable` (boolean)
*   **Methods:** `setCashFail()`, `setBankingFail()`, `setBothFail()`, `isCashAvailable(): boolean`, `isBankingAvailable(): boolean`, `processQR(double fee): boolean`, `processCash(double fee): boolean`

### 10. Sensor
Simulates an IoT parking slot occupancy sensor.
*   **Location:** `com.se.sebtl.service.iot.Sensor`
*   **Attributes:** `slotId` (int), `iotManager` (IoTManagerService), `internalState` (SlotStatus)
*   **Methods:** `Sensor(int slotId, IoTManagerService iotManager)` (Constructor), `reportAvailable()`, `reportOccupied()`, `reportFailure()`, `restoreState()`

## Relationships

*   **IoTManagerService** uses several repositories to persist parking slot and ticket states. It also tracks a list of `IntersectionSign` (IoT hardware device representation) and orchestrates them.
*   **ParkingService** provides data to controllers, acting as an abstraction over `TicketView`, `ParkingSlot`, and `Price` repositories.
*   **SecurityService** provides token parsing and validation, fetching `AppUser` data to ensure access control.
*   **HardwareSimulatorService** is closely coupled with **IoTManagerService**. It initializes `Sensor` and `IntersectionSign` instances, providing them with a reference to the `IoTManagerService` to report status changes.
*   **Sensor** and **IntersectionSign** interact directly with **IoTManagerService** to report occupancy changes, device health, or update directional signaling.
*   **Camera**, **CardReader**, **Gate**, and **PaymentSlot** offer independent simulated hardware functionalities primarily consumed by controller layers handling authentication and entry/exit logic.
