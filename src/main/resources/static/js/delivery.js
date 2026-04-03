const deliveryTokenKeys = ["jwtToken", "token", "authToken"];

const deliveryAssignmentsList = document.getElementById("deliveryAssignmentsList");
const refreshDeliveriesBtn = document.getElementById("refreshDeliveriesBtn");

function deliveryHeaders(extra = {}) {
  const deliveryToken = deliveryTokenKeys.map((key) => localStorage.getItem(key)).find((v) => !!v) || null;
  if (!deliveryToken) {
    return extra;
  }
  return {
    ...extra,
    Authorization: `Bearer ${deliveryToken}`
  };
}

async function parseDeliveryError(response, fallbackMessage) {
  try {
    const data = await response.json();
    if (data && typeof data.message === "string" && data.message.trim()) {
      return data.message;
    }
  } catch (error) {
    // Ignore parse errors.
  }
  return fallbackMessage;
}

function deliveryCardTemplate(item) {
  const inTransitAction = item.status === "ASSIGNED"
    ? '<button class="approve-btn" data-action="IN_TRANSIT" type="button">Mark In Transit</button>'
    : "";
  const deliveredAction = (item.status === "ASSIGNED" || item.status === "IN_TRANSIT")
    ? '<button class="chip-btn" data-action="DELIVERED" type="button">Mark Delivered</button>'
    : "";

  return `
    <article class="delivery-card" data-id="${item.id}">
      <h4>${item.requestedBookTitle} <small>(offered: ${item.offeredBookTitle || "-"})</small></h4>
      <p>Requester: ${item.requesterUsername}</p>
      <p>Owner: ${item.ownerUsername}</p>
      <p>Status: <span class="status-chip">${item.status}</span></p>
      ${item.deliveryManUsername ? `<p>Assigned to: ${item.deliveryManUsername}</p>` : ""}
      ${item.assignedAt ? `<p>Assigned at: ${new Date(item.assignedAt).toLocaleString()}</p>` : ""}
      ${item.deliveredAt ? `<p>Delivered at: ${new Date(item.deliveredAt).toLocaleString()}</p>` : ""}
      <div class="inline-actions">${inTransitAction}${deliveredAction}</div>
    </article>
  `;
}

async function fetchAssignments() {
  let response = await fetch("/api/delivery/my-assignments", {
    method: "GET",
    credentials: "include",
    headers: deliveryHeaders()
  });

  if ((response.status === 401 || response.status === 403) && localStorage.getItem("jwtToken")) {
    response = await fetch("/api/delivery/my-assignments", {
      method: "GET",
      credentials: "include"
    });
  }

  if (!response.ok) {
    throw new Error(await parseDeliveryError(response, "Unable to load delivery assignments."));
  }

  return response.json();
}

async function loadAssignments() {
  try {
    const items = await fetchAssignments();
    deliveryAssignmentsList.innerHTML = items.length
      ? items.map(deliveryCardTemplate).join("")
      : "<p>No deliveries assigned yet.</p>";
  } catch (error) {
    deliveryAssignmentsList.innerHTML = `<p>${error.message || "Unable to load delivery assignments."}</p>`;
  }
}

async function updateDeliveryStatus(id, status) {
  let response = await fetch(`/api/delivery/${id}/status`, {
    method: "PATCH",
    credentials: "include",
    headers: deliveryHeaders({
      "Content-Type": "application/json"
    }),
    body: JSON.stringify({ status })
  });

  if ((response.status === 401 || response.status === 403) && localStorage.getItem("jwtToken")) {
    response = await fetch(`/api/delivery/${id}/status`, {
      method: "PATCH",
      credentials: "include",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ status })
    });
  }

  if (!response.ok) {
    throw new Error(await parseDeliveryError(response, "Could not update delivery status."));
  }
}

document.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLButtonElement)) {
    return;
  }

  const status = target.dataset.action;
  if (!status) {
    return;
  }

  const card = target.closest(".delivery-card");
  if (!card) {
    return;
  }

  const id = card.getAttribute("data-id");
  if (!id) {
    return;
  }

  try {
    await updateDeliveryStatus(id, status);
    await loadAssignments();
  } catch (error) {
    card.insertAdjacentHTML("beforeend", `<p>${error.message || "Update failed."}</p>`);
  }
});

if (refreshDeliveriesBtn) {
  refreshDeliveriesBtn.addEventListener("click", () => {
    void loadAssignments();
  });
}

void loadAssignments();
