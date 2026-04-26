// auth.js
const token = sessionStorage.getItem("sebtl_token");
const role = sessionStorage.getItem("sebtl_role");

console.log("Auth Check - Token:", token, "Role:", role);

function requireAuth(expectedRole) {
    if (!token) {
        logout();
        throw new Error("No token found. Redirecting to login.");
    }
    if (expectedRole && role !== expectedRole) {
        alert("Unauthorized access. Redirecting.");
        logout();
        throw new Error("Invalid Role.");
    }
}

function logout() {
    sessionStorage.clear();
    window.location.href = "/login";
}

// Automatically inject a logout button into the body
document.addEventListener("DOMContentLoaded", () => {
    const btn = document.createElement("button");
    btn.innerText = "Logout";
    btn.style.cssText = "background: #dc3545; color: white; float: right; padding: 8px 15px; border: none; cursor: pointer;";
    btn.onclick = logout;
    document.body.insertBefore(btn, document.body.firstChild);
});

// Helper for authorized fetches
const authHeaders = { 
    "Authorization": token,
    "Content-Type": "application/json"
};


// --- GLOBAL UI ERROR TOAST ---
function showErrorToast(message) {
    let toast = document.getElementById("sebtl-error-toast");
    
    // If it doesn't exist on this page yet, create it dynamically
    if (!toast) {
        toast = document.createElement("div");
        toast.id = "sebtl-error-toast";
        // Modern CSS styling for a floating banner
        toast.style.cssText = "position: fixed; top: 20px; left: 50%; transform: translateX(-50%); background-color: #dc3545; color: white; padding: 15px 30px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); z-index: 10000; font-family: sans-serif; font-weight: bold; text-align: center; opacity: 0; transition: opacity 0.3s ease-in-out; pointer-events: none;";
        document.body.appendChild(toast);
    }

    // Set the message and fade it in
    toast.innerText = "⚠️ " + message;
    toast.style.opacity = "1";

    // Auto-hide after 5 seconds
    setTimeout(() => {
        toast.style.opacity = "0";
    }, 5000);
}


async function handleResponse(response) {
    if (!response.ok) {
        if (response.status === 401) {
            alert("Missing or bad authentication. Please log in again.");
            logout();
            return;
        }
        if (response.status === 403) {
            alert("You do not have permission to perform this action.");
            logout();
            return;
        }
        
        try {
            // Try to read the exact error message from our Spring Boot GlobalExceptionHandler
            const err = await response.json();
            showErrorToast(err.message || "A system error occurred.");
            throw new Error(err.message);
        } catch (e) {
            // Fallback if the server crashed so hard it didn't return JSON
            showErrorToast("Server is unreachable (HTTP " + response.status + ")");
            throw new Error("HTTP " + response.status);
        }
    }
    return response;
}

