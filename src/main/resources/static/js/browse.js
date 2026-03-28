const browseTokenKeys = ["jwtToken", "token", "authToken"];
const browseToken = browseTokenKeys.map((key) => localStorage.getItem(key)).find((v) => !!v) || null;

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
