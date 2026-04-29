const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const axios = require('axios');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Connect to SQLite (Created locally inside the local_storage folder and ignored by Git)
const db = new sqlite3.Database(__dirname + '/edge_database.db', (err) => {
    if (err) console.error("Database opening error: ", err);
});

const MAIN_SERVER = 'http://localhost:18080';

// Initialize SQLite tables
db.serialize(() => {
    // Queue for POST/PUT/PATCH requests when offline
    db.run(`CREATE TABLE IF NOT EXISTS request_queue (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        method TEXT,
        url TEXT,
        body TEXT,
        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);

    // Cache for GET requests (like active-tickets, prices)
    db.run(`CREATE TABLE IF NOT EXISTS data_cache (
        endpoint TEXT PRIMARY KEY,
        response_data TEXT,
        last_sync DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);
});

let SIMULATE_OFFLINE = false;

app.post('/api/edge-config/offline', (req, res) => {
    SIMULATE_OFFLINE = !!req.body.offline;
    console.log(`[EDGE CONFIG] Network disconnect simulated: ${SIMULATE_OFFLINE}`);
    res.json({ success: true, offline: SIMULATE_OFFLINE });
});

let isSyncing = false;

// 1a. State Pull Background Service: Fetch latest ground-truth from Central DB
let isPulling = false;
setInterval(async () => {
    if (isPulling) return; 
    isPulling = true;
    try {
        if (SIMULATE_OFFLINE) throw new Error("Simulated offline mode");
        
        // Enforce a strict 5-second timeout so hanging cloud connections never exhaust local hardware sockets
        const response = await axios.get(MAIN_SERVER + '/api/simulation/active-tickets', { timeout: 5000 });
        db.run(`INSERT OR REPLACE INTO data_cache (endpoint, response_data, last_sync) VALUES (?, ?, CURRENT_TIMESTAMP)`, 
        ['/api/simulation/active-tickets', JSON.stringify(response.data)]);
    } catch (err) {
        // If main server is slow or offline, gracefully fail and retain the last snapshot.
    } finally {
        isPulling = false; // Always release the lock
    }
}, 15000); // Relaxed to 15 seconds to reduce cross-ocean bandwidth

// 1b. Event Push Background Service: Push queued actions to Central DB (every 5 seconds)
setInterval(() => {
    if (isSyncing) return;
    isSyncing = true;
    
    // Try to flush offline queue
    db.all(`SELECT * FROM request_queue ORDER BY id ASC`, [], async (err, rows) => {
        if (err || rows.length === 0) {
            isSyncing = false;
            return;
        }
        
        console.log(`[SYNC] Found ${rows.length} pending events offline. Attempting to push to main server...`);
        for (const row of rows) {
            if (SIMULATE_OFFLINE) {
                console.log(`[SYNC] Simulated offline active. Stopping sync.`);
                break;
            }
            let parsedBody;
            try {
                parsedBody = JSON.parse(row.body);
            } catch (e) {
                console.error(`[SYNC] Failed to parse queued request body for id ${row.id}. Deleting malformed request.`);
                db.run(`DELETE FROM request_queue WHERE id = ?`, [row.id]);
                continue;
            }

            try {
                await axios({
                    method: row.method,
                    url: MAIN_SERVER + row.url,
                    data: parsedBody
                });
                console.log(`[SYNC] Successfully pushed event ${row.id} (${row.method} ${row.url})`);
                // Delete on success
                db.run(`DELETE FROM request_queue WHERE id = ?`, [row.id]);
            } catch (e) {
                console.log(`[SYNC] Main server still unreachable. Stopping sync.`);
                break; // Stop syncing until next interval, keep queue order intact
            }
        }
        isSyncing = false;
    });
}, 5000);

// 2. Main Proxy Middleware (Intercepts ALL traffic)
app.all('*', async (req, res) => {
    const endpoint = req.originalUrl;
    
    // Ignore internal edge-config routes
    if (endpoint.startsWith('/api/edge-config')) return res.status(404).send();
    
    try {
        if (SIMULATE_OFFLINE) {
            throw new Error("Simulated offline mode");
        }
        
        // Happy Path: Server is ONLINE
        const response = await axios({
            method: req.method,
            url: MAIN_SERVER + endpoint,
            data: req.method === 'GET' ? undefined : req.body
        });
        
        // If it was a GET, opportunistically update our edge cache
        if (req.method === 'GET') {
            db.run(`INSERT OR REPLACE INTO data_cache (endpoint, response_data, last_sync) VALUES (?, ?, CURRENT_TIMESTAMP)`, 
            [endpoint, JSON.stringify(response.data)]);
        }
        
        return res.status(response.status).send(response.data);
        
    } catch (error) {
        // Main Server is OFFLINE or threw an error without a response
        if (!error.response) {
            console.log(`[EDGE] Main server unreachable for ${req.method} ${endpoint}. Engaging offline Edge mode.`);
            
            if (req.method === 'GET') {
                // Serve from SQLite cache
                db.get(`SELECT response_data FROM data_cache WHERE endpoint = ?`, [endpoint], (err, row) => {
                    if (row) {
                        console.log(`[EDGE] Serving ${endpoint} from local SQLite cache.`);
                        return res.json(JSON.parse(row.response_data));
                    } else {
                        return res.status(503).json({ error: "Main server down and no local cache available." });
                    }
                });
            } else {
                // It's a POST/PUT. Check if it's an entrance and verify plate locally!
                if (req.method === 'POST' && endpoint.startsWith('/api/simulation/entrance')) {
                    const urlObj = new URL(endpoint, 'http://localhost');
                    let plate = urlObj.searchParams.get('existingPlate');
                    if (!plate) {
                        // Assign a local Edge offline plate since backend camera cannot run
                        plate = "EDGE-OFFLINE-" + Math.floor(Math.random() * 10000);
                        urlObj.searchParams.set('existingPlate', plate);
                        endpoint = urlObj.pathname + urlObj.search;
                    }

                    // 1. Check local cache (snapshot of active tickets before going offline)
                    db.get(`SELECT response_data FROM data_cache WHERE endpoint = '/api/simulation/active-tickets'`, (err, row) => {
                        let isActive = false;
                        if (row) {
                            const tickets = JSON.parse(row.response_data);
                            isActive = tickets.some(t => t.licensePlate === plate);
                        }
                        
                        // 2. Playback recent offline actions to get absolute current local state
                        db.all(`SELECT url FROM request_queue WHERE url LIKE '%/api/simulation/entrance%' OR url LIKE '%/api/simulation/exit%' ORDER BY id ASC`, (err, qRows) => {
                            if (qRows) {
                                for (const qRow of qRows) {
                                    if (qRow.url.includes('/api/simulation/entrance') && qRow.url.includes(plate)) {
                                        isActive = true;
                                    } else if (qRow.url.includes('/api/simulation/exit') && qRow.url.includes(plate)) {
                                        isActive = false;
                                    }
                                }
                            }

                            // 3. Verdict
                            if (isActive) {
                                console.log(`[EDGE SECURITY REJECT] Denied offline entrance for existing plate: ${plate}`);
                                return res.status(400).json({ error: "[EDGE LOCAL VERIFICATION] Exception: Plate already exists in lot: " + plate });
                            }

                            // Proceed to queue safely
                            db.run(`INSERT INTO request_queue (method, url, body) VALUES (?, ?, ?)`, 
                            [req.method, endpoint, JSON.stringify(req.body || {})], function(err) {
                                if (err) return res.status(500).json({ error: "Edge DB failure." });
                                
                                console.log(`[EDGE] Buffered offline action safely to disk: ${req.method} ${endpoint}`);
                                return res.status(200).json({ 
                                    success: true, 
                                    message: `[OFFLINE QUEUED] Processed locally. Plate: ${plate}`,
                                    ticketId: Math.floor(Math.random() * 10000),
                                    slotId: -1
                                });
                            });
                        });
                    });
                    return; // Halt base fallback
                }

                // Default queue fallback for all other actions (exits, assignments, etc)
                db.run(`INSERT INTO request_queue (method, url, body) VALUES (?, ?, ?)`, 
                [req.method, endpoint, JSON.stringify(req.body || {})], function(err) {
                    if (err) {
                        return res.status(500).json({ error: "Edge DB failure." });
                    }
                    
                    console.log(`[EDGE] Buffered offline action safely to disk: ${req.method} ${endpoint}`);
                    return res.status(200).json({ 
                        success: true, 
                        message: "[OFFLINE QUEUED] Processed by Edge Node locally.",
                        ticketId: Math.floor(Math.random() * 10000),
                        slotId: -1
                    });
                });
            }
        } else {
            // Main Server is ONLINE but returned an actual HTTP error (e.g. 400 Bad Request)
            return res.status(error.response.status).send(error.response.data);
        }
    }
});

// Start Edge Proxy
const PORT = 3000;
app.listen(PORT, () => {
    console.log(`-----------------------------------------------------`);
    console.log(`Edge Proxy Server running on http://localhost:${PORT}`);
    console.log(`Routing all traffic safely to ${MAIN_SERVER}`);
    console.log(`If connection drops, data is queued to local SQLite.`);
    console.log(`-----------------------------------------------------`);
});