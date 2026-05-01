// Server-side Filter + Search (Thymeleaf reload)

document.addEventListener('DOMContentLoaded', function () {
    initFilters();
    initSearch();
    initSorting();
    initDenominationToggle(); // for christian/converted_christian
});

// =====================
// Filters
// =====================
function initFilters() {
    const filterForm = document.getElementById('filterForm');
    if (!filterForm) return;

    // Apply filters on change
    const filterInputs = filterForm.querySelectorAll('select, input[type="checkbox"], input[type="text"]');

    filterInputs.forEach(input => {
        input.addEventListener('change', function () {
            applyFilters();
        });
    });

    // Reset filters
    const resetBtn = document.getElementById('resetFilters');
    if (resetBtn) {
        resetBtn.addEventListener('click', function (e) {
            e.preventDefault();
            resetFilters();
        });
    }
}

function applyFilters() {
    const filterForm = document.getElementById('filterForm');
    if (!filterForm) return;

    const formData = new FormData(filterForm);
    const params = new URLSearchParams();

    // Keep existing query params (like q, sort, page) if you want:
    // (optional) You can comment this block if you want to always reset those.
    const currentParams = new URLSearchParams(window.location.search);
    for (const [k, v] of currentParams.entries()) {
        if (v != null && String(v).trim() !== "") {
            params.set(k, v);
        }
    }

    // Apply new filter values (overwrite)
    for (const [key, value] of formData.entries()) {
        const v = (value == null) ? "" : String(value).trim();

        // checkboxes: only include if checked
        const el = filterForm.querySelector(`[name="${CSS.escape(key)}"]`);
        const isCheckbox = el && el.type === "checkbox";

        if (isCheckbox) {
            if (el.checked) params.set(key, el.value || "true");
            else params.delete(key);
            continue;
        }

        // normal fields
        if (v !== "") params.set(key, v);
        else params.delete(key);
    }

    // Remove page when filters change (so pagination resets)
    params.delete("page");

    window.location.href = `${window.location.pathname}?${params.toString()}`;
}

function resetFilters() {
    window.location.href = window.location.pathname;
}

// =====================
// Search (server-side using q=...)
// =====================
function initSearch() {
    const searchInput = document.getElementById('profileSearch');
    if (!searchInput) return;

    const debouncedSearch = window.matrimonyApp && window.matrimonyApp.debounce
        ? window.matrimonyApp.debounce(redirectSearch, 500)
        : debounceFallback(redirectSearch, 500);

    searchInput.addEventListener('input', function () {
        debouncedSearch(this.value);
    });
}

function redirectSearch(query) {
    const q = (query || "").trim();
    const params = new URLSearchParams(window.location.search);

    // Only search if empty or >= 3 chars
    if (q.length === 0) {
        params.delete("q");
    } else if (q.length >= 3) {
        params.set("q", q);
    } else {
        return;
    }

    // Reset page on new search
    params.delete("page");

    window.location.href = `${window.location.pathname}?${params.toString()}`;
}

// =====================
// Sorting (server-side using sort=...)
// =====================
function initSorting() {
    const sortSelect = document.getElementById('sortBy');
    if (!sortSelect) return;

    sortSelect.addEventListener('change', function () {
        const params = new URLSearchParams(window.location.search);
        const sort = (this.value || "").trim();

        if (sort) params.set("sort", sort);
        else params.delete("sort");

        params.delete("page");
        window.location.href = `${window.location.pathname}?${params.toString()}`;
    });
}

// =====================
// Denomination toggle (show only if christian / converted_christian selected)
// =====================
function initDenominationToggle() {
    const religion = document.getElementById('religion');
    const denomGroup = document.getElementById('denominationGroup');
    const denom = document.getElementById('denomination');

    if (!religion || !denomGroup) return;

    function toggleDenomination() {
        const r = (religion.value || "").trim();
        const show = (r === 'christian' || r === 'converted_christian');

        denomGroup.style.display = show ? '' : 'none';
        if (!show && denom) denom.value = '';
    }

    religion.addEventListener('change', toggleDenomination);
    toggleDenomination(); // run on load
}

// =====================
// Fallback debounce (if main.js not loaded)
// =====================
function debounceFallback(func, wait) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}
