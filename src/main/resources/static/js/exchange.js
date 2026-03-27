const exchangeTokenKeys = ["jwtToken", "token", "authToken"];
const exchangeToken = exchangeTokenKeys.map((key) => localStorage.getItem(key)).find((v) => !!v) || null;

const myRequestsList = document.getElementById("myRequestsList");
const ownerRequestsList = document.getElementById("ownerRequestsList");
const moderationRequestsList = document.getElementById("moderationRequestsList");
const refreshModerationBtn = document.getElementById("refreshModerationBtn");

function exchangeHeaders(extra = {}) {
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
  return `
    <article class="request-card" data-id="${item.id}">
      <h4>${item.bookTitle} <small>(offered: ${item.offeredBookTitle || "-"})</small></h4>
      <p>Requester: ${item.requesterUsername} | Owner: ${item.bookOwnerUsername}</p>
      <p>Status: ${statusChip(item.status)}</p>
      <p>Message: ${item.message || "-"}</p>
      ${item.reviewedByUsername ? `<p>Reviewed by: ${item.reviewedByUsername}</p>` : ""}
      ${actions}
    </article>
  `;
}

async function fetchJson(url) {
  const response = await fetch(url, {
    method: "GET",
    credentials: "include",
    headers: exchangeHeaders()
  });

  if (!response.ok) {
    throw new Error("Request failed");
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
      const actions = item.status === "PENDING"
        ? `<div class="inline-actions"><button class="cancel-btn" data-action="cancel" type="button">Cancel</button></div>`
        : "";
      return requestCardTemplate(item, actions);
    }).join("");
  } catch (error) {
    myRequestsList.innerHTML = "<p>Unable to load your requests.</p>";
  }
}

async function loadOwnerRequests() {
  try {
    const items = await fetchJson("/api/exchange-requests/my-book-requests");
    ownerRequestsList.innerHTML = items.length
      ? items.map((item) => requestCardTemplate(item)).join("")
      : "<p>No incoming requests for your books yet.</p>";
  } catch (error) {
    ownerRequestsList.innerHTML = "<p>Unable to load owner-side requests.</p>";
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
    moderationRequestsList.innerHTML = "<p>Moderator queue is only visible to moderators/admins.</p>";
  }
}

async function actOnRequest(id, action) {
  const response = await fetch(`/api/exchange-requests/${id}/${action}`, {
    method: "PATCH",
    credentials: "include",
    headers: exchangeHeaders()
  });

  if (!response.ok) {
    throw new Error("Action failed");
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
    card.insertAdjacentHTML("beforeend", "<p>Action failed. Try again.</p>");
  }
});

refreshModerationBtn.addEventListener("click", () => {
  void loadModerationQueue();
});

void Promise.all([loadMyRequests(), loadOwnerRequests(), loadModerationQueue()]);
