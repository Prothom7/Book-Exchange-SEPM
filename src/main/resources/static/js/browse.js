const browseTokenKeys = ["jwtToken", "token", "authToken"];
const BROWSE_PAGE_SIZE = 12;
const exchangeModal = document.getElementById("exchangeRequestModal");
const exchangeRequestForm = document.getElementById("exchangeRequestForm");
const exchangeRequestedBookIdInput = document.getElementById("requestedBookId");
const exchangeOfferedBookSelect = document.getElementById("offeredBookId");
const exchangeMessageInput = document.getElementById("exchangeMessage");
const exchangeModalStatus = document.getElementById("exchangeModalStatus");
const exchangeModalSubtitle = document.getElementById("exchangeModalSubtitle");
const exchangeModalSubmit = document.getElementById("exchangeModalSubmit");
let activeExchangeCard = null;

function browseHeaders(extra = {}) {
  const browseToken = browseTokenKeys.map((key) => localStorage.getItem(key)).find((v) => !!v) || null;
  if (!browseToken) {
    return extra;
  }
  return {
    ...extra,
    Authorization: `Bearer ${browseToken}`
  };
}

async function getMyBooksForOffer() {
  const response = await fetch("/api/books/my-books", {
    method: "GET",
    credentials: "include",
    headers: browseHeaders()
  });

  if (!response.ok) {
    throw new Error("Could not load own books");
  }

  const books = await response.json();
  return books.filter((book) => book.available);
}

function setCardStatus(card, message, isError = false) {
  const status = card.querySelector(".card-status");
  if (!status) {
    return;
  }
  status.textContent = message;
  status.style.color = isError ? "#8e2b2b" : "#405171";
}

function setExchangeModalStatus(message, isError = false) {
  if (!exchangeModalStatus) {
    return;
  }
  exchangeModalStatus.textContent = message;
  exchangeModalStatus.style.color = isError ? "#a12626" : "#405171";
}

function closeExchangeModal() {
  if (!exchangeModal) {
    return;
  }
  exchangeModal.classList.add("hidden");
  exchangeModal.setAttribute("aria-hidden", "true");
  exchangeRequestForm?.reset();
  exchangeOfferedBookSelect.innerHTML = '<option value="">Select one of your books</option>';
  setExchangeModalStatus("");
  activeExchangeCard = null;
}

function openExchangeModal(requestedBookId, requestedBookTitle, myBooks, card) {
  if (!exchangeModal || !exchangeRequestedBookIdInput || !exchangeOfferedBookSelect) {
    return;
  }

  exchangeRequestedBookIdInput.value = String(requestedBookId);
  exchangeModalSubtitle.textContent = `Request "${requestedBookTitle}" by offering one of your available books.`;
  exchangeOfferedBookSelect.innerHTML = '<option value="">Select one of your books</option>';
  myBooks.forEach((book) => {
    const option = document.createElement("option");
    option.value = String(book.id);
    option.textContent = `${book.id}: ${book.title}`;
    exchangeOfferedBookSelect.appendChild(option);
  });

  setExchangeModalStatus("");
  activeExchangeCard = card;
  exchangeModal.classList.remove("hidden");
  exchangeModal.setAttribute("aria-hidden", "false");
  exchangeOfferedBookSelect.focus();
}

document.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLButtonElement)) {
    return;
  }

  const card = target.closest(".book-card");
  if (!card) {
    return;
  }

  if (target.classList.contains("wishlist-btn")) {
    const payload = {
      bookTitle: target.dataset.title || "",
      author: target.dataset.author || null,
      genre: target.dataset.genre || null
    };

    try {
      const response = await fetch("/api/wishlist/subscribe", {
        method: "POST",
        credentials: "include",
        headers: browseHeaders({
          "Content-Type": "application/json"
        }),
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        throw new Error("Wishlist failed");
      }

      setCardStatus(card, "Added to wishlist subscription.");
    } catch (error) {
      setCardStatus(card, "Could not add to wishlist.", true);
    }
  }

  if (target.classList.contains("request-btn")) {
    const requestedBookId = target.dataset.bookId;
    const requestedBookTitle = target.dataset.bookTitle;

    try {
      const myBooks = await getMyBooksForOffer();
      if (!myBooks.length) {
        setCardStatus(card, "Add and mark at least one of your books as available first.", true);
        return;
      }
      openExchangeModal(requestedBookId, requestedBookTitle, myBooks, card);
    } catch (error) {
      setCardStatus(card, error?.message || "Could not submit exchange request.", true);
    }
  }
});

exchangeRequestForm?.addEventListener("submit", async (event) => {
  event.preventDefault();

  const requestedBookId = Number(exchangeRequestedBookIdInput?.value);
  const offeredBookId = Number(exchangeOfferedBookSelect?.value);
  const message = exchangeMessageInput?.value?.trim() || "";

  if (!requestedBookId || Number.isNaN(offeredBookId)) {
    setExchangeModalStatus("Choose one of your books before sending the request.", true);
    return;
  }

  try {
    exchangeModalSubmit.disabled = true;
    setExchangeModalStatus("Sending exchange request...");

    const response = await fetch("/api/exchange-requests", {
      method: "POST",
      credentials: "include",
      headers: browseHeaders({
        "Content-Type": "application/json"
      }),
      body: JSON.stringify({
        bookId: requestedBookId,
        offeredBookId,
        message
      })
    });

    if (!response.ok) {
      const payload = await response.json().catch(() => null);
      throw new Error(payload?.message || "Request failed");
    }

    if (activeExchangeCard) {
      setCardStatus(activeExchangeCard, "Exchange request submitted for moderator review.");
    }
    closeExchangeModal();
  } catch (error) {
    setExchangeModalStatus(error?.message || "Could not submit exchange request.", true);
  } finally {
    exchangeModalSubmit.disabled = false;
  }
});

document.getElementById("exchangeModalCancel")?.addEventListener("click", closeExchangeModal);
document.getElementById("exchangeModalClose")?.addEventListener("click", closeExchangeModal);
exchangeModal?.addEventListener("click", (event) => {
  const target = event.target;
  if (target instanceof HTMLElement && target.dataset.closeModal === "true") {
    closeExchangeModal();
  }
});

function initBrowsePagination() {
  const cards = Array.from(document.querySelectorAll(".books-grid .book-card"));
  const paginationRoot = document.getElementById("browsePagination");

  if (!paginationRoot || cards.length === 0) {
    return;
  }

  const totalPages = Math.ceil(cards.length / BROWSE_PAGE_SIZE);
  if (totalPages <= 1) {
    paginationRoot.innerHTML = "";
    return;
  }

  function renderPage(pageNumber) {
    const start = (pageNumber - 1) * BROWSE_PAGE_SIZE;
    const end = start + BROWSE_PAGE_SIZE;

    cards.forEach((card, index) => {
      card.style.display = index >= start && index < end ? "" : "none";
    });

    const buttons = Array.from(paginationRoot.querySelectorAll(".browse-page-btn"));
    buttons.forEach((button) => {
      button.classList.toggle("active", Number(button.dataset.page) === pageNumber);
    });
  }

  paginationRoot.innerHTML = "";
  for (let page = 1; page <= totalPages; page += 1) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "browse-page-btn";
    button.dataset.page = String(page);
    button.textContent = String(page);
    button.addEventListener("click", () => renderPage(page));
    paginationRoot.appendChild(button);
  }

  renderPage(1);
}

initBrowsePagination();
