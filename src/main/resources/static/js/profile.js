// Profile Management JavaScript (Server-rendered safe version)

document.addEventListener('DOMContentLoaded', function () {
    initImageUpload();
    initProfileForm();
    initProfileActions();
});

// ---------------------
// Helpers
// ---------------------
function app() {
    return window.matrimonyApp || {
        showAlert: function () {},
        showLoading: function () {},
        hideLoading: function () {},
        showFieldError: function () {},
        removeFieldError: function () {}
    };
}

function isLoggedIn() {
    // body attribute is like data-logged-in="true" or "false"
    const v = document.body ? document.body.getAttribute('data-logged-in') : null;
    return v === 'true';
}

// ---------------------
// Image Upload Preview
// ---------------------
function initImageUpload() {
    const fileInput = document.getElementById('profileImage');
    const imagePreview = document.querySelector('.image-preview');

    if (!fileInput || !imagePreview) return;

    fileInput.addEventListener('change', function (e) {
        const file = e.target.files && e.target.files[0];
        if (!file) return;

        if (file.size > 5 * 1024 * 1024) {
            app().showAlert('Image size should be less than 5MB', 'error');
            fileInput.value = '';
            return;
        }

        if (!file.type.startsWith('image/')) {
            app().showAlert('Please select a valid image file', 'error');
            fileInput.value = '';
            return;
        }

        const reader = new FileReader();
        reader.onload = function (event) {
            imagePreview.innerHTML = `<img src="${event.target.result}" alt="Profile Preview"
                style="width:100%;height:100%;object-fit:cover;border-radius:8px;">`;
        };
        reader.readAsDataURL(file);
    });
}

// ---------------------
// Profile Form Validation
// ---------------------
function initProfileForm() {
    const profileForm = document.getElementById('profileForm');
    if (!profileForm) return;

    profileForm.addEventListener('submit', function (e) {
        if (!validateProfileForm()) {
            e.preventDefault();
            app().showAlert('Please fill all required fields correctly', 'error');
        }
    });

    const inputs = profileForm.querySelectorAll('input, select, textarea');
    inputs.forEach(input => {
        input.addEventListener('blur', function () {
            validateField(this);
        });
    });
}

function validateField(field) {
    const value = (field.value || '').trim();
    const fieldName = field.name || '';
    let isValid = true;
    let errorMessage = '';

    if (field.hasAttribute('required') && !value) {
        isValid = false;
        errorMessage = 'This field is required';
    }

    if (fieldName === 'email' && value) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(value)) {
            isValid = false;
            errorMessage = 'Please enter a valid email address';
        }
    }

    if (fieldName === 'phone' && value) {
        const digits = value.replace(/\D/g, '');
        if (!/^[0-9]{10}$/.test(digits)) {
            isValid = false;
            errorMessage = 'Please enter a valid 10-digit phone number';
        }
    }

    if (fieldName === 'age' && value) {
        const age = parseInt(value, 10);
        if (isNaN(age) || age < 18 || age > 100) {
            isValid = false;
            errorMessage = 'Age must be between 18 and 100';
        }
    }

    if (!isValid) {
        field.classList.add('error');
        app().showFieldError(field, errorMessage);
    } else {
        field.classList.remove('error');
        app().removeFieldError(field);
    }

    return isValid;
}

function validateProfileForm() {
    const form = document.getElementById('profileForm');
    if (!form) return true;

    const requiredFields = form.querySelectorAll('[required]');
    let isValid = true;

    requiredFields.forEach(field => {
        if (!validateField(field)) isValid = false;
    });

    return isValid;
}

// ---------------------
// Profile Actions
// ---------------------
function initProfileActions() {
    // View Profile Button
    document.querySelectorAll('.btn-view-profile').forEach(btn => {
        btn.addEventListener('click', function (e) {
            const profileId = this.getAttribute('data-profile-id');
            if (!profileId) return;

            // If it is a <button>, prevent default; for <a>, let it work
            e.preventDefault();
            window.location.href = `/profile/${profileId}`;
        });
    });

    // Contact Button
    document.querySelectorAll('.btn-contact').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();

            if (!isLoggedIn()) {
                app().showAlert('Please login to contact profiles', 'info');
                setTimeout(() => window.location.href = '/login', 1200);
                return;
            }

            // Server-side Thymeleaf already controls contact visibility.
            // So just scroll to contact section if present.
            const contactSection = document.querySelector('.details-section .fa-lock-open') ||
                                   document.querySelector('.details-section .fa-lock');

            if (contactSection) {
                contactSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
            } else {
                app().showAlert('Contact section not available on this page.', 'info');
            }
        });
    });

    // Shortlist Button (server-side fallback)
    document.querySelectorAll('.btn-shortlist').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();

            if (!isLoggedIn()) {
                app().showAlert('Please login to shortlist profiles', 'info');
                setTimeout(() => window.location.href = '/login', 1200);
                return;
            }

            // No API implemented yet. Show message for now.
            // Later we can add backend endpoint and do real shortlist save.
            app().showAlert('Shortlist feature will be enabled after backend API setup.', 'info');
        });
    });
}
