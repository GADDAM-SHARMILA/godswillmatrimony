/**
 * Shortlist functionality for matrimony application
 */

document.addEventListener("DOMContentLoaded", function () {

    const shortlistBtns = document.querySelectorAll(".btn-shortlist");

    loadShortlistCount();

    shortlistBtns.forEach(btn => {
        btn.addEventListener("click", function (e) {
            e.preventDefault();

            const profileId = this.getAttribute("data-profile-id");

            if (!profileId) {
                console.error("Profile ID not found");
                return;
            }

            toggleShortlist(profileId, btn);
        });
    });

});


// ------------------ LOAD BADGE COUNT ------------------

function loadShortlistCount() {
    const badge = document.getElementById("shortlist-badge");
    if (!badge) return;

    fetch("/shortlist/api/count", {
        method: "GET",
        credentials: "same-origin"   // IMPORTANT
    })
        .then(res => {
            if (!res.ok) return null;
            return res.json();
        })
        .then(data => {
            if (!data) return;

            if (data.count > 0) {
                badge.textContent = data.count;
                badge.style.display = "inline-block";
            } else {
                badge.style.display = "none";
            }
        })
        .catch(err => console.error("Error loading shortlist count:", err));
}


// ------------------ TOGGLE SHORTLIST ------------------

function toggleShortlist(profileId, buttonElement) {

    const loggedIn = document.body.getAttribute("data-logged-in");

    if (loggedIn === "false") {
        window.location.href = "/login";
        return;
    }

    buttonElement.disabled = true;
    const originalHTML = buttonElement.innerHTML;

    buttonElement.innerHTML =
        '<i class="fas fa-spinner fa-spin"></i> Loading...';

    fetch(`/api/shortlist/toggle/${profileId}`, {
        method: "POST",
        credentials: "same-origin"   // IMPORTANT
    })
        .then(res => {
            if (!res.ok) {
                if (res.status === 401) {
                    throw new Error("Please login first");
                }
                throw new Error("Server error");
            }
            return res.json();
        })
        .then(data => {

            if (data.success) {

                if (data.shortlisted) {
                    buttonElement.classList.add("shortlisted");
                    buttonElement.innerHTML =
                        '<i class="fas fa-heart"></i> <span>Shortlisted</span>';
                    showNotification("Added to shortlist", "success");

                } else {
                    buttonElement.classList.remove("shortlisted");
                    buttonElement.innerHTML =
                        '<i class="fas fa-heart"></i> <span>Shortlist</span>';
                    showNotification("Removed from shortlist", "info");
                }

                loadShortlistCount();
            }
        })
        .catch(err => {
            console.error(err);
            buttonElement.innerHTML = originalHTML;
            showNotification(err.message, "error");
        })
        .finally(() => {
            buttonElement.disabled = false;
        });
}


// ------------------ NOTIFICATION ------------------

function showNotification(message, type = "info") {

    const notification = document.createElement("div");
    notification.textContent = message;

    const colors = {
        success: "#28a745",
        error: "#dc3545",
        info: "#17a2b8"
    };

    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        border-radius: 8px;
        color: white;
        font-weight: 500;
        z-index: 10000;
        background: ${colors[type]};
        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
    `;

    document.body.appendChild(notification);

    setTimeout(() => notification.remove(), 3000);
}


// ------------------ BUTTON STYLE ------------------

if (!document.querySelector("style[data-shortlist-style]")) {
    const style = document.createElement("style");
    style.setAttribute("data-shortlist-style", "true");

    style.textContent = `
        .btn-shortlist.shortlisted {
            background-color: #e85d75 !important;
            color: white !important;
        }
    `;

    document.head.appendChild(style);
}