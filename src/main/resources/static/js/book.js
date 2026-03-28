const tokenKeys = ["jwtToken", "token", "authToken"];
const createForm = document.getElementById("bookCreateForm");
const createStatus = document.getElementById("bookCreateStatus");
const myBooksList = document.getElementById("myBooksList");
const refreshBooksBtn = document.getElementById("refreshBooksBtn");

let activeToken = tokenKeys.map((key) => localStorage.getItem(key)).find((v) => !!v) || null;

function withAuthHeaders(extra = {}) {
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
  myBooksList.innerHTML = "<p>Loading your library...</p>";

  try {
    const response = await fetch("/api/books/my-books", {
      method: "GET",
      credentials: "include",
      headers: withAuthHeaders()
    });

    if (!response.ok) {
      myBooksList.innerHTML = "<p>Unable to load your books.</p>";
      return;
    }

    const books = await response.json();
    if (!books.length) {
      myBooksList.innerHTML = "<p>You have not listed any books yet.</p>";
      return;
    }

    myBooksList.innerHTML = books.map((book) => `
      <article class="book-item" data-book-id="${book.id}">
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
  } catch (error) {
    myBooksList.innerHTML = "<p>Failed to load your library.</p>";
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

  const parent = target.closest(".book-item");
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
