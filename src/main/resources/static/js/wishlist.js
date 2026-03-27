const wishlistTokenKeys = ["jwtToken", "token", "authToken"];
const wishlistToken = wishlistTokenKeys.map((key) => localStorage.getItem(key)).find((v) => !!v) || null;

const wishlistForm = document.getElementById("wishlistForm");
const wishlistStatus = document.getElementById("wishlistStatus");
const wishlistItems = document.getElementById("wishlistItems");
const notificationItems = document.getElementById("notificationItems");

function wishlistHeaders(extra = {}) {
  if (!wishlistToken) {
    return extra;
  }
  return {
    ...extra,
    Authorization: `Bearer ${wishlistToken}`
  };
}

function setWishlistStatus(message, kind = "") {
  wishlistStatus.textContent = message;
  wishlistStatus.classList.remove("error", "success");
  if (kind) {
    wishlistStatus.classList.add(kind);
  }
}

async function getJson(url) {
  const response = await fetch(url, {
    method: "GET",
    credentials: "include",
    headers: wishlistHeaders()
  });

  if (!response.ok) {
    throw new Error("Fetch failed");
  }

  return response.json();
}

async function loadWishlist() {
  try {
    const items = await getJson("/api/wishlist/my");
    if (!items.length) {
      wishlistItems.innerHTML = "<p>No wishlist subscriptions yet.</p>";
      return;
    }

    wishlistItems.innerHTML = items.map((item) => `
      <article class="list-item" data-id="${item.id}">
        <h4>${item.bookTitle}</h4>
        <p>Author filter: ${item.author || "Any"}</p>
        <p>Genre filter: ${item.genre || "Any"}</p>
        <p>Status: ${item.active ? "Active" : "Inactive"}</p>
        ${item.active ? '<button type="button" data-action="deactivate">Deactivate</button>' : ""}
      </article>
    `).join("");
  } catch (error) {
    wishlistItems.innerHTML = "<p>Could not load wishlist subscriptions.</p>";
  }
}

async function loadNotifications() {
  try {
    const items = await getJson("/api/notifications/my");
    if (!items.length) {
      notificationItems.innerHTML = "<p>No notifications yet.</p>";
      return;
    }

    notificationItems.innerHTML = items.map((item) => `
      <article class="list-item ${item.read ? "notification-read" : ""}" data-id="${item.id}">
        <h4>${item.bookTitle || "Book availability"}</h4>
        <p>${item.message}</p>
        <p>Received: ${new Date(item.createdAt).toLocaleString()}</p>
        ${item.read ? "" : '<button type="button" data-action="read">Mark as read</button>'}
      </article>
    `).join("");
  } catch (error) {
    notificationItems.innerHTML = "<p>Could not load notifications.</p>";
  }
}

wishlistForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  setWishlistStatus("Subscribing...");

  const formData = new FormData(wishlistForm);
  const payload = {
    bookTitle: formData.get("bookTitle"),
    author: formData.get("author") || null,
    genre: formData.get("genre") || null
  };

  try {
    const response = await fetch("/api/wishlist/subscribe", {
      method: "POST",
      credentials: "include",
      headers: wishlistHeaders({
        "Content-Type": "application/json"
      }),
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new Error("Subscribe failed");
    }

    wishlistForm.reset();
    setWishlistStatus("Subscription created.", "success");
    await loadWishlist();
  } catch (error) {
    setWishlistStatus("Could not create subscription.", "error");
  }
});

document.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLButtonElement)) {
    return;
  }

  const action = target.dataset.action;
  if (!action) {
    return;
  }

  const card = target.closest(".list-item");
  if (!card) {
    return;
  }

  const id = card.getAttribute("data-id");
  if (!id) {
    return;
  }

  try {
    if (action === "deactivate") {
      const response = await fetch(`/api/wishlist/${id}/deactivate`, {
        method: "PATCH",
        credentials: "include",
        headers: wishlistHeaders()
      });
      if (!response.ok) {
        throw new Error("Deactivate failed");
      }
      await loadWishlist();
    }

    if (action === "read") {
      const response = await fetch(`/api/notifications/${id}/read`, {
        method: "PATCH",
        credentials: "include",
        headers: wishlistHeaders()
      });
      if (!response.ok) {
        throw new Error("Read failed");
      }
      await loadNotifications();
    }
  } catch (error) {
    setWishlistStatus("Action failed. Try again.", "error");
  }
});

void Promise.all([loadWishlist(), loadNotifications()]);
