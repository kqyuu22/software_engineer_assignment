// shared.js
// Shared functions between Admin and Operator dashboards

// Bulletproof array fetcher
async function safeFetchArray(url) {
    try {
        const response = await fetch(url, { headers: authHeaders });
        console.log(`API Response from ${url}:`, response);

        await handleResponse(response)

        const data = await response.json();
        return Array.isArray(data) ? data : [];
    } catch (error) {
        console.error(`Fetch error at ${url}:`, error);
        return [];
    }
}

async function loadTickets() {
    document.getElementById("searchPlate").value = "";
    renderTickets(await safeFetchArray(`${API_BASE}/history`));
}

async function searchTickets() {
    const query = document.getElementById("searchPlate").value.trim();
    if (!query) return loadTickets();
    console.log(`Searching tickets for: ${query}`);
    renderTickets(await safeFetchArray(`${API_BASE}/history/search?query=${query}`));
}

function renderTickets(tickets) {
    let html = "";
    tickets.forEach(t => {
        const status = t.finished ? "Finished" : "Active";
        html += `<tr><td>${t.ticketId}</td><td>${t.userId}</td><td>${t.licensePlate}</td><td>${new Date(t.entryTime).toLocaleString()}</td><td>${t.exitTime ? new Date(t.exitTime).toLocaleString() : '-'}</td><td>${status}</td></tr>`;
    });
    document.getElementById("tickets-body").innerHTML = html;
}
