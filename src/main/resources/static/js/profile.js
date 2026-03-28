const tokenKeys = ["jwtToken", "token", "authToken"];

const profileImageEl = document.getElementById("profileImage");
const profileImageInputEl = document.getElementById("profileImageInput");
const saveImageBtnEl = document.getElementById("saveImageBtn");
const profileUsernameEl = document.getElementById("profileUsername");
const profileEmailEl = document.getElementById("profileEmail");
const profileRolesEl = document.getElementById("profileRoles");
const profileMetaEl = document.getElementById("profileMeta");
const profileStatusEl = document.getElementById("profileStatus");
const logoutBtnEl = document.getElementById("logoutBtn");

let activeToken = getStoredToken();
let pendingImageDataUrl = null;

function getStoredToken() {
  for (const key of tokenKeys) {
    const token = localStorage.getItem(key);
    if (token) {
      return token;
    }
  }
  return null;
}

function clearStoredTokens() {
  tokenKeys.forEach((key) => localStorage.removeItem(key));
}

function withAuthHeaders(baseHeaders = {}) {
  if (activeToken) {
    return {
      ...baseHeaders,
      Authorization: `Bearer ${activeToken}`
    };
  }
  return baseHeaders;
}

function setStatus(message, type = "") {
  profileStatusEl.textContent = message;
  profileStatusEl.classList.remove("error", "success");
  if (type) {
    profileStatusEl.classList.add(type);
  }
}

function fallbackAvatar(username = "Reader") {
  const safeName = (username || "Reader").trim();
  const initials = safeName.slice(0, 2).toUpperCase();
  const svg = `
    <svg xmlns='http://www.w3.org/2000/svg' width='300' height='300' viewBox='0 0 300 300'>
      <defs>
        <linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>
          <stop offset='0%' stop-color='#dce8ff' />
          <stop offset='100%' stop-color='#e9f8ee' />
        </linearGradient>
      </defs>
      <rect width='300' height='300' fill='url(#g)' rx='36' />
      <text x='150' y='170' text-anchor='middle' font-size='110' font-family='Segoe UI, sans-serif' fill='#455986'>${initials}</text>
    </svg>
  `;

  return `data:image/svg+xml;base64,${btoa(svg)}`;
}

function renderRoles(roles = []) {
  profileRolesEl.innerHTML = "";
  if (!Array.isArray(roles) || roles.length === 0) {
    return;
  }

  roles.forEach((role) => {
    const chip = document.createElement("span");
    chip.className = "role-chip";
    chip.textContent = role.replace("ROLE_", "");
    profileRolesEl.appendChild(chip);
  });
}

function formatDate(dateValue) {
  if (!dateValue) {
    return "-";
  }

  const parsed = new Date(dateValue);
  if (Number.isNaN(parsed.getTime())) {
    return "-";
  }

  return parsed.toLocaleString();
}

async function loadProfile() {
  setStatus("Loading profile...");

  try {
    const response = await fetch("/api/user/profile", {
      method: "GET",
      headers: withAuthHeaders(),
      credentials: "include"
    });

    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        setStatus("JWT token is missing, invalid, or expired. Please log in again.", "error");
        return;
      }
      setStatus("Unable to load profile right now.", "error");
      return;
    }

    const profile = await response.json();
    profileUsernameEl.textContent = profile.username || "Reader";
    profileEmailEl.textContent = profile.email || "No email available";
    renderRoles(profile.roles || []);

    const createdAt = formatDate(profile.createdAt);
    const updatedAt = formatDate(profile.updatedAt);
    profileMetaEl.textContent = `Member since: ${createdAt} | Last updated: ${updatedAt}`;

    profileImageEl.src = profile.profileImageDataUrl || fallbackAvatar(profile.username);
    profileImageEl.alt = `${profile.username || "Reader"} profile image`;
    setStatus("Profile loaded.", "success");
  } catch (error) {
    setStatus("Profile request failed. Please try again.", "error");
  }
}

function readImageAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error("File read failed"));
    reader.readAsDataURL(file);
  });
}

profileImageInputEl.addEventListener("change", async (event) => {
  const file = event.target.files?.[0];
  if (!file) {
    return;
  }

  if (!file.type.startsWith("image/")) {
    setStatus("Please select a valid image file.", "error");
    return;
  }

  const maxSizeBytes = 2 * 1024 * 1024;
  if (file.size > maxSizeBytes) {
    setStatus("Please choose an image smaller than 2 MB.", "error");
    return;
  }

  try {
    pendingImageDataUrl = await readImageAsDataUrl(file);
    profileImageEl.src = pendingImageDataUrl;
    saveImageBtnEl.disabled = false;
    setStatus("Image selected. Click Save Image to update profile.");
  } catch (error) {
    setStatus("Could not read selected image.", "error");
  }
});

saveImageBtnEl.addEventListener("click", async () => {
  if (!pendingImageDataUrl) {
    return;
  }

  setStatus("Updating profile image...");

  try {
    const response = await fetch("/api/user/profile-image", {
      method: "PUT",
      headers: withAuthHeaders({
        "Content-Type": "application/json"
      }),
      credentials: "include",
      body: JSON.stringify({
        imageDataUrl: pendingImageDataUrl
      })
    });

    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        setStatus("Your session/token is not valid. Please login again.", "error");
      } else {
        setStatus("Could not update profile image.", "error");
      }
      return;
    }

    const updatedProfile = await response.json();
    profileImageEl.src = updatedProfile.profileImageDataUrl || profileImageEl.src;
    pendingImageDataUrl = null;
    profileImageInputEl.value = "";
    saveImageBtnEl.disabled = true;
    setStatus("Profile image updated successfully.", "success");
  } catch (error) {
    setStatus("Profile image update failed. Please try again.", "error");
  }
});

logoutBtnEl.addEventListener("click", async () => {
  clearStoredTokens();

  try {
    await fetch("/logout", {
      method: "POST",
      credentials: "include"
    });
  } catch (error) {
    // Continue to login regardless of logout endpoint response.
  }

  window.location.href = "/login?logout=true";
});

void loadProfile();
