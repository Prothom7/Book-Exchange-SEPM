const browseTokenKeys = ["jwtToken", "token", "authToken"];
const browseToken = browseTokenKeys.map((key) => localStorage.getItem(key)).find((v) => !!v) || null;
const BROWSE_PAGE_SIZE = 12;

function browseHeaders(extra = {}) {
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

      const optionsText = myBooks.map((book) => `${book.id}: ${book.title}`).join("\n");
      const selected = window.prompt(`Offer one of your books by ID:\n${optionsText}`);
      if (!selected) {
        return;
      }

      const offeredBookId = Number(selected);
      if (Number.isNaN(offeredBookId)) {
        setCardStatus(card, "Invalid offered book ID.", true);
        return;
      }

      const message = window.prompt(`Optional note for requesting '${requestedBookTitle}':`) || "";

      const response = await fetch("/api/exchange-requests", {
        method: "POST",
        credentials: "include",
        headers: browseHeaders({
          "Content-Type": "application/json"
        }),
        body: JSON.stringify({
          bookId: Number(requestedBookId),
          offeredBookId,
          message
        })
      });

      if (!response.ok) {
        throw new Error("Request failed");
      }

      setCardStatus(card, "Exchange request submitted for moderator review.");
    } catch (error) {
      setCardStatus(card, "Could not submit exchange request.", true);
    }
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
