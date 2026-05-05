# Frontend Integration & Technical Specifications
This document outlines the authentication flow, edge case handling, and role-based page requirements for the frontend application.

## Important things to note first


### Session Authentication
-	Server does NOT store information about a client’s session
-	When a client logins, they sends their info (e.g. username, password) to the server
-	The server receives, and then generates a token based on username, user id, roles,… including session expiration date too. The server sends the token to the client
-	The client receives the token, and for every subsequent page (e.g. after the client logins, the client is redirected to the main page), the client must send the token in header Authorization to the server. The server receive this token, decoding the token, and verify the data in the token

#### Frontend Handling for Session Expiration
Because the backend rigidly enforces expiration, the frontend only *strictly needs* to handle the reactive case, but proactive handling improves UX:
- **Reactive validation (Required)**: If the token expires, the backend will inevitably reject the request with a `401 Unauthorized` status. The global Axios/Fetch interceptor must catch this 401 error, clear the stored session data, alert the user ("Session expired. Please log in again."), and redirect to the login page.
- **Proactive validation (Optional UX)**: The frontend *can* parse the token's `Expr` date (ISO-8601 format) to silently clear the session or show a warning popup right before it expires. This prevents the user from trying to submit a form or click a button only to be hit with a 401 error.

### Edge case and Error Test
-	Disable one controller API => Frontend has problem fetching from backend
-	Disable connection to the database
-	Failing to store data? Lightweight queue system in local storage? Idk, this is frontend problem or edge device problem
-	Ctrl+Shift+I => Application => Session Storage => Change the role => Unauthorized error
-	Change the token => Invalid token exception => 401 error

### **Global Exception Handling**
Every failed request (4xx or 5xx) will return a JSON object with a single key: `error`. You should create a 'Global Interceptor' in your Axios or Fetch setup to catch this `error` string and display it in a Snackbar/Toast component. We have covered common errors:
- 401 Unauthorized: For invalid or expired tokens.
- 403 Forbidden: When a user has a valid token but lacks the specific role (e.g., a Member trying to access Admin slots).
- 404 Not Found: For missing resources like a specific paymentId
- Invalid Credentials (401): Wrong username or password during login.
- Database Down (500): The backend can’t reach the DB.
- Malformed Request (400): The frontend sent a string where a number was expected, or the JSON body is missing required fields.

## Application

### 1. Login Page (Default) `/login`

- Features: Input form for username and password.


- Success Logic: Redirect to the corresponding dashboard based on the role (ADMIN, MEMBER, or OPERATOR).


- Failure Logic: Display error regarding authentication.

### 2. Member Page `/member`

- Access Control: Validate token and role; redirect to login if missing or incorrect.

- Features:
    - Ticket History: `GET /member/history`.
        - Displays tickets ordered by entry time.
        - UX Enhancement: If the latest ticket is not finished, display current parking status (duration, etc.).
    - Payment History: `GET /member/payment`.
        - Lists payments from latest to oldest.
        - Provides a "Pay" button for unpaid items (POST /member/payment/{paymentId}).

### 3. Operator Page `/operator`
- Access Control: Token and role validation required.
- Features:
    - Real-Time View: `GET operator/slots/`
        - Send to frontend a list of slots ordered by ascending slot id.
        - Based on the slot info, front end display the whole map of the parking slot

        | Section | ID Range |
        |---------|----------|
        |    A    |   1-50   |
        |    B    |  51-100  |
        |    C    | 101-150  |
        |    D    | 151-200  |
        |    E    | 201-250  |

    - Alert Management
        - Active: `GET operator/alerts/active` (unresolved). You can use a hook like useQuery to refresh every 30 seconds. If none, then frontend receives an empty array [].
        - History: `GET operator/alerts/history` (resolved).
        - Action: Resolve active alerts via `PATCH operator/alerts/{alertId}/resolve`
        - *Note*: we have 2 differect routes for resolved alerts and unresolved alerts, and so, we should have to separate sections showing Resolved Alerts and Unresolved Alerts
    - Ticket History
        - Omni Search: `GET operator/history/search?query=${query}` to search by user ID, ticket, or license plate.
        - List of all tickets ordered by entry time `GET operator/history`

### 4. Admin Page `/admin`
- Access Control: Token and role validation required.
- Features:
    - Price Management
        - See current price `GET /admin/price` 
        - Update price based on priority (STAFF, LECTURER, STUDENT, OTHER) `PUT /admin/price?priority=${priority}&newPrice=${p}`
    - Slot Management
        - Slot View  `GET /admin/slots`: **Exactly as Operator Page**
        - Slot Priority Update `PATCH /admin/slots/bulk`: Currently support
            - Update 1 slot, e.g. Input `1` choose STAFF or `2` choose LECTURER.
            - Update many different slots, e.g. Input `1, 10, 123` choose STAFF.
            - Update many consecutive slots, e.g. Input `1-100` choose STAFF means updating the priority to STAFF for 100 slots (from 1 to 100).
    - Ticket History: **Exactly as Operator Page**
        - Omni Search: `GET operator/history/search?query=${query}` to search by user ID, ticket, or license plate.
        - List of all tickets ordered by entry time `GET operator/history`


### 5. Hardware Simulation Page
Read `HARDWARE_SIMULATION.md` for full details.


## Backend Implementation Status
The backend has implemented the following core architectural features to support frontend development:
- Route normalization for the main entry points (`/login, /member, /operator, /admin`). Any request to the root `localhost:8080` is automatically redirected to the `/login` view
- All functional API endpoints (e.g., `/auth/login`, `/operator/slots`) are strictly contracted to return data in **JSON format**. *Read the return format of the controllers for extra sure!*
- Every failed request (4xx or 5xx) returns a JSON object with a single consistent key `error`. Read folder `ApiErrorResponse.java` and `GlobalExceptionHandler.java` for the specific details of the returned format (and suggest improvement if possible)
- Already handling CORS
- Still developing...

