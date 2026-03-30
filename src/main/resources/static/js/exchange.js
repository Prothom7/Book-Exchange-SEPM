const exchangeTokenKeys = ["jwtToken", "token", "authToken"];

const myRequestsList = document.getElementById("myRequestsList");
const ownerRequestsList = document.getElementById("ownerRequestsList");
const moderationRequestsList = document.getElementById("moderationRequestsList");
const refreshModerationBtn = document.getElementById("refreshModerationBtn");

function exchangeHeaders(extra = {}) {
  const exchangeToken = exchangeTokenKeys.map((key) => localStorage.getItem(key)).find((v) => !!v) || null;
  if (!exchangeToken) {
    return extra;
  }
  return {
    ...extra,
    Authorization: `Bearer ${exchangeToken}`
  };
}

function statusChip(status) {
  return `<span class="status-chip">${status}</span>`;
}

function requestCardTemplate(item, actions = "") {
  const fallbackCover = "https://placehold.co/260x380/eef2ff/334155?text=Book+Cover";
  const requestedCover = item.bookImageUrl || fallbackCover;
  const offeredCover = item.offeredBookImageUrl || fallbackCover;

  return `
    <article class="request-card" data-id="${item.id}">
      <div class="exchange-covers" style="display:flex;gap:10px;margin-bottom:8px;">
        <img src="${requestedCover}" alt="Requested book cover" style="width:46px;height:64px;object-fit:cover;border-radius:6px;" onerror="this.onerror=null;this.src='${fallbackCover}';">
        <img src="${offeredCover}" alt="Offered book cover" style="width:46px;height:64px;object-fit:cover;border-radius:6px;" onerror="this.onerror=null;this.src='${fallbackCover}';">
      </div>
      <h4>${item.bookTitle} <small>(offered: ${item.offeredBookTitle || "-"})</small></h4>
      <p>Requester: ${item.requesterUsername} | Owner: ${item.bookOwnerUsername}</p>
      <p>Status: ${statusChip(item.status)}</p>
      <p>Message: ${item.message || "-"}</p>
      ${item.reviewedByUsername ? `<p>Reviewed by: ${item.reviewedByUsername}</p>` : ""}
      ${actions}
    </article>
  `;
}

async function parseErrorMessage(response, fallbackMessage) {
  try {
    const data = await response.json();
    if (data && typeof data.message === "string" && data.message.trim()) {
      return data.message;
    }
  } catch (error) {
    // Ignore non-JSON responses.
  }
  return fallbackMessage;
}

async function fetchJson(url) {
  let response = await fetch(url, {
    method: "GET",
    credentials: "include",
    headers: exchangeHeaders()
  });

  if ((response.status === 401 || response.status === 403) && localStorage.getItem("jwtToken")) {
    response = await fetch(url, {
      method: "GET",
      credentials: "include"
    });
  }

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "Request failed"));
  }

  return response.json();
}

async function loadMyRequests() {
  try {
    const items = await fetchJson("/api/exchange-requests/my-requests");
    if (!items.length) {
      myRequestsList.innerHTML = "<p>No exchange requests created yet.</p>";
      return;
    }

    myRequestsList.innerHTML = items.map((item) => {
      let actions = "";
      if (item.status === "PENDING") {
        const acceptBtn = item.requesterAcceptedAt
          ? ""
          : `<button class="approve-btn" data-action="accept" type="button">Accept</button>`;
        actions = `<div class="inline-actions">${acceptBtn}<button class="cancel-btn" data-action="cancel" type="button">Cancel</button></div>`;
      }
      return requestCardTemplate(item, actions);
    }).join("");
  } catch (error) {
    myRequestsList.innerHTML = `<p>${error.message || "Unable to load your requests."}</p>`;
  }
}

async function loadOwnerRequests() {
  try {
    const items = await fetchJson("/api/exchange-requests/my-book-requests");
    ownerRequestsList.innerHTML = items.length
      ? items.map((item) => {
          const actions = item.status === "PENDING"
            ? (item.ownerAcceptedAt
              ? '<div class="inline-actions"><span class="status-chip">Waiting for moderator review</span></div>'
              : '<div class="inline-actions"><button class="approve-btn" data-action="accept" type="button">Accept</button></div>')
            : "";
          return requestCardTemplate(item, actions);
        }).join("")
      : "<p>No incoming requests for your books yet.</p>";
  } catch (error) {
    ownerRequestsList.innerHTML = `<p>${error.message || "Unable to load owner-side requests."}</p>`;
  }
}

async function loadModerationQueue() {
  try {
    const items = await fetchJson("/api/exchange-requests/moderation/pending");
    moderationRequestsList.innerHTML = items.length
      ? items.map((item) => requestCardTemplate(
          item,
          '<div class="inline-actions">' +
            '<button class="approve-btn" data-action="approve" type="button">Approve</button>' +
            '<button class="reject-btn" data-action="reject" type="button">Reject</button>' +
          '</div>'
        )).join("")
      : "<p>No pending requests in moderator queue.</p>";
  } catch (error) {
    moderationRequestsList.innerHTML = `<p>${error.message || "Moderator queue is only visible to moderators/admins."}</p>`;
  }
}

async function actOnRequest(id, action) {
  let response = await fetch(`/api/exchange-requests/${id}/${action}`, {
    method: "PATCH",
    credentials: "include",
    headers: exchangeHeaders()
  });

  if ((response.status === 401 || response.status === 403) && localStorage.getItem("jwtToken")) {
    response = await fetch(`/api/exchange-requests/${id}/${action}`, {
      method: "PATCH",
      credentials: "include"
    });
  }

  if (!response.ok) {
    throw new Error(await parseErrorMessage(response, "Action failed"));
  }
}

document.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLButtonElement)) {
    return;
  }

  const action = target.dataset.action;
  if (!action) {
    return;
  }

  const card = target.closest(".request-card");
  if (!card) {
    return;
  }

  const id = card.getAttribute("data-id");
  if (!id) {
    return;
  }

  try {
    await actOnRequest(id, action);
    await Promise.all([loadMyRequests(), loadOwnerRequests(), loadModerationQueue()]);
  } catch (error) {
    card.insertAdjacentHTML("beforeend", `<p>${error.message || "Action failed. Try again."}</p>`);
  }
});

if (refreshModerationBtn) {
  refreshModerationBtn.addEventListener("click", () => {
    void loadModerationQueue();
  });
}

void Promise.all([loadMyRequests(), loadOwnerRequests(), loadModerationQueue()]);
