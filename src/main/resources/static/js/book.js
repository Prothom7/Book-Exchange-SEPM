const tokenKeys = ["jwtToken", "token", "authToken"];
const createForm = document.getElementById("bookCreateForm");
const createStatus = document.getElementById("bookCreateStatus");
const myBooksList = document.getElementById("myBooksList");
const exchangeBooksList = document.getElementById("exchangeBooksList");
const refreshBooksBtn = document.getElementById("refreshBooksBtn");

let activeToken = tokenKeys.map((key) => localStorage.getItem(key)).find((v) => !!v) || null;

function withAuthHeaders(extra = {}) {
  activeToken = tokenKeys.map((key) => localStorage.getItem(key)).find((v) => !!v) || null;
  if (!activeToken) {
    return extra;
  }
  return {
    ...extra,
    Authorization: `Bearer ${activeToken}`
  };
}

function setStatus(message, kind = "") {
  createStatus.textContent = message;
  createStatus.classList.remove("error", "success");
  if (kind) {
    createStatus.classList.add(kind);
  }
}

async function loadMyBooks() {
  myBooksList.innerHTML = "<p>Loading your books...</p>";
  exchangeBooksList.innerHTML = "<p>Loading exchange shelf...</p>";

  try {
    const response = await fetch("/api/books/my-books", {
      method: "GET",
      credentials: "include",
      headers: withAuthHeaders()
    });

    if (!response.ok) {
      myBooksList.innerHTML = "<p>Unable to load your books.</p>";
      exchangeBooksList.innerHTML = "<p>Unable to load exchange shelf.</p>";
      return;
    }

    const books = await response.json();
    if (!books.length) {
      myBooksList.innerHTML = "<p>You have not listed any books yet.</p>";
      exchangeBooksList.innerHTML = "<p>No books available for exchange yet.</p>";
      return;
    }

    const placeholder = "https://placehold.co/260x380/eef2ff/334155?text=Book+Cover";
    const getCover = (book) => book.imageUrl || placeholder;

    myBooksList.innerHTML = books.map((book) => `
      <article class="book-shelf-card" data-book-id="${book.id}">
        <img class="book-cover" src="${getCover(book)}" alt="Cover of ${book.title}" onerror="this.onerror=null;this.src='${placeholder}';">
        <h4>${book.title}</h4>
        <p>Author: ${book.author}</p>
        <p>ISBN: ${book.isbn}</p>
        <div class="item-row">
          <span class="availability-state">${book.available ? "Available for exchange" : "Unavailable"}</span>
          <button class="availability-btn" data-available="${book.available}" type="button">
            Mark ${book.available ? "Unavailable" : "Available"}
          </button>
        </div>
      </article>
    `).join("");

    const availableBooks = books.filter((book) => book.available);
    if (!availableBooks.length) {
      exchangeBooksList.innerHTML = "<p>No books available for exchange yet.</p>";
      return;
    }

    exchangeBooksList.innerHTML = availableBooks.map((book) => `
      <article class="book-shelf-card">
        <img class="book-cover" src="${getCover(book)}" alt="Cover of ${book.title}" onerror="this.onerror=null;this.src='${placeholder}';">
        <h4>${book.title}</h4>
        <p>Author: ${book.author}</p>
        <p>Condition: ${book.condition || "N/A"}</p>
        <p class="availability-state">Available for exchange</p>
      </article>
    `).join("");
  } catch (error) {
    myBooksList.innerHTML = "<p>Failed to load your library.</p>";
    exchangeBooksList.innerHTML = "<p>Failed to load exchange shelf.</p>";
  }
}

async function createBook(payload) {
  const response = await fetch("/api/books", {
    method: "POST",
    credentials: "include",
    headers: withAuthHeaders({
      "Content-Type": "application/json"
    }),
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    throw new Error("Create failed");
  }

  return response.json();
}

createForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  setStatus("Adding book...");

  const formData = new FormData(createForm);
  const payload = {
    title: formData.get("title"),
    author: formData.get("author"),
    genre: formData.get("genre") || "General",
    isbn: formData.get("isbn"),
    condition: formData.get("condition"),
    description: formData.get("description")
  };

  try {
    await createBook(payload);
    createForm.reset();
    setStatus("Book listed successfully.", "success");
    await loadMyBooks();
  } catch (error) {
    setStatus("Could not add book. Please verify fields and try again.", "error");
  }
});

myBooksList.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLButtonElement)) {
    return;
  }

  if (!target.classList.contains("availability-btn")) {
    return;
  }

  const parent = target.closest(".book-shelf-card");
  if (!parent) {
    return;
  }

  const bookId = parent.getAttribute("data-book-id");
  const currentlyAvailable = target.getAttribute("data-available") === "true";
  const next = !currentlyAvailable;

  try {
    const response = await fetch(`/api/books/${bookId}/availability?available=${next}`, {
      method: "PATCH",
      credentials: "include",
      headers: withAuthHeaders()
    });

    if (!response.ok) {
      throw new Error("Toggle failed");
    }

    await loadMyBooks();
  } catch (error) {
    setStatus("Could not change availability.", "error");
  }
});

refreshBooksBtn.addEventListener("click", () => {
  void loadMyBooks();
});

void loadMyBooks();
