const chatTokenKeys = ["jwtToken", "token", "authToken"];

const startConversationForm = document.getElementById("startConversationForm");
const chatUsernameInput = document.getElementById("chatUsername");
const chatUserSuggestions = document.getElementById("chatUserSuggestions");
const conversationList = document.getElementById("conversationList");
const activeConversationTitle = document.getElementById("activeConversationTitle");
const chatConnectionState = document.getElementById("chatConnectionState");
const chatMessageList = document.getElementById("chatMessageList");
const chatSendForm = document.getElementById("chatSendForm");
const chatMessageInput = document.getElementById("chatMessageInput");
const chatStatus = document.getElementById("chatStatus");

let stompClient = null;
let isConnected = false;
let currentUserId = null;
let currentUsername = null;
let activeConversation = null;
let renderedMessageIds = new Set();
let searchDebounce = null;

function getStoredToken() {
  for (const key of chatTokenKeys) {
    const token = localStorage.getItem(key);
    if (token) {
      return token;
    }
  }
  return null;
}

function authHeaders(extra = {}) {
  const token = getStoredToken();
  if (!token) {
    return extra;
  }
  return {
    ...extra,
    Authorization: `Bearer ${token}`
  };
}

function setStatus(message, kind = "") {
  chatStatus.textContent = message;
  chatStatus.classList.remove("error", "success");
  if (kind) {
    chatStatus.classList.add(kind);
  }
}

function setConnectionState(message, kind = "") {
  chatConnectionState.textContent = message;
  chatConnectionState.classList.remove("good", "warn");
  if (kind) {
    chatConnectionState.classList.add(kind);
  }
}

function escapeHtml(value) {
  return (value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function formatTime(value) {
  if (!value) {
    return "";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "";
  }
  return parsed.toLocaleString();
}

function messageBelongsToActiveConversation(message) {
  if (!activeConversation || !message) {
    return false;
  }
  const senderId = Number(message.senderId);
  const receiverId = Number(message.receiverId);
  const peerId = Number(activeConversation.id);
  return (senderId === peerId && receiverId === currentUserId)
    || (senderId === currentUserId && receiverId === peerId);
}

function appendMessageBubble(message, scrollToBottom = false) {
  const messageId = Number(message.id);
  if (!Number.isNaN(messageId) && renderedMessageIds.has(messageId)) {
    return;
  }
  if (!Number.isNaN(messageId)) {
    renderedMessageIds.add(messageId);
  }

  const sentByMe = Number(message.senderId) === currentUserId;
  const bubble = document.createElement("article");
  bubble.className = `chat-bubble ${sentByMe ? "sent" : "received"}`;

  const content = document.createElement("p");
  content.innerHTML = escapeHtml(message.content);
  bubble.appendChild(content);

  const timestamp = document.createElement("time");
  timestamp.textContent = formatTime(message.timestamp);
  bubble.appendChild(timestamp);

  if (sentByMe && message.deliveredToReceiver === false) {
    const hint = document.createElement("div");
    hint.className = "delivery-hint";
    hint.textContent = "Receiver appears offline. Message stored and will be visible in history.";
    bubble.appendChild(hint);
  }

  chatMessageList.appendChild(bubble);

  if (scrollToBottom) {
    chatMessageList.scrollTop = chatMessageList.scrollHeight;
  }
}

function renderConversationList(items) {
  if (!Array.isArray(items) || items.length === 0) {
    conversationList.innerHTML = "<p>No conversations yet.</p>";
    return;
  }

  conversationList.innerHTML = items.map((item) => {
    const isActive = activeConversation && Number(activeConversation.id) === Number(item.participantId);
    const lastPrefix = item.lastMessageSentByMe ? "You: " : "";
    return `
      <button type="button" class="conversation-item ${isActive ? "active" : ""}" data-id="${item.participantId}" data-username="${escapeHtml(item.participantUsername)}">
        <h4>${escapeHtml(item.participantUsername)}</h4>
        <p>${escapeHtml(lastPrefix + (item.lastMessage || ""))}</p>
        <p>${formatTime(item.lastMessageTimestamp)}</p>
      </button>
    `;
  }).join("");
}

async function loadCurrentUser() {
  const response = await fetch("/api/user/profile", {
    method: "GET",
    credentials: "include",
    headers: authHeaders()
  });

  if (!response.ok) {
    throw new Error("Could not load current user profile");
  }

  const profile = await response.json();
  currentUserId = Number(profile.id);
  currentUsername = profile.username;
}

async function loadConversations() {
  try {
    const response = await fetch("/api/chat/conversations", {
      method: "GET",
      credentials: "include",
      headers: authHeaders()
    });

    if (!response.ok) {
      throw new Error("Unable to fetch conversations");
    }

    const conversations = await response.json();
    renderConversationList(conversations);
  } catch (error) {
    conversationList.innerHTML = "<p>Unable to load conversations right now.</p>";
  }
}

async function loadMessages(participant) {
  activeConversation = participant;
  activeConversationTitle.textContent = `Chat with ${participant.username}`;
  renderedMessageIds = new Set();
  chatMessageList.innerHTML = "<p class='chat-placeholder'>Loading messages...</p>";

  try {
    const response = await fetch(`/api/chat/messages/${participant.id}`, {
      method: "GET",
      credentials: "include",
      headers: authHeaders()
    });

    if (!response.ok) {
      throw new Error("Could not load conversation messages");
    }

    const messages = await response.json();
    chatMessageList.innerHTML = "";

    if (!messages.length) {
      chatMessageList.innerHTML = "<p class='chat-placeholder'>No messages yet. Say hello.</p>";
      return;
    }

    messages.forEach((message) => appendMessageBubble(message));
    chatMessageList.scrollTop = chatMessageList.scrollHeight;
  } catch (error) {
    chatMessageList.innerHTML = "<p class='chat-placeholder'>Could not load messages.</p>";
  }
}

function connectWebSocket() {
  if (typeof SockJS === "undefined" || typeof Stomp === "undefined") {
    setConnectionState("Real-time module unavailable. Falling back to HTTP sends.", "warn");
    return;
  }

  const socket = new SockJS("/ws-chat");
  stompClient = Stomp.over(socket);
  stompClient.debug = () => {};

  const token = getStoredToken();
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  stompClient.connect(
    headers,
    () => {
      isConnected = true;
      setConnectionState("Connected in real-time", "good");

      stompClient.subscribe("/user/queue/messages", (frame) => {
        const message = JSON.parse(frame.body);

        if (messageBelongsToActiveConversation(message)) {
          if (chatMessageList.querySelector(".chat-placeholder")) {
            chatMessageList.innerHTML = "";
          }
          appendMessageBubble(message, true);
        }

        void loadConversations();
      });
    },
    () => {
      isConnected = false;
      setConnectionState("Realtime disconnected. HTTP fallback enabled.", "warn");
    }
  );
}

async function sendViaHttpFallback(payload) {
  const response = await fetch("/api/chat/messages", {
    method: "POST",
    credentials: "include",
    headers: authHeaders({
      "Content-Type": "application/json"
    }),
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    throw new Error("Fallback send failed");
  }

  return response.json();
}

startConversationForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const username = (chatUsernameInput.value || "").trim();
  if (!username) {
    return;
  }

  setStatus("Starting conversation...");

  try {
    const response = await fetch("/api/chat/conversations/start", {
      method: "POST",
      credentials: "include",
      headers: authHeaders({
        "Content-Type": "application/json"
      }),
      body: JSON.stringify({ username })
    });

    if (!response.ok) {
      throw new Error("Could not start conversation");
    }

    const participant = await response.json();
    await loadConversations();
    await loadMessages(participant);
    setStatus("Conversation ready.", "success");
  } catch (error) {
    setStatus("Could not start conversation. Check username and try again.", "error");
  }
});

chatUsernameInput.addEventListener("input", () => {
  const query = (chatUsernameInput.value || "").trim();
  if (searchDebounce) {
    window.clearTimeout(searchDebounce);
  }

  if (query.length < 2) {
    chatUserSuggestions.innerHTML = "";
    return;
  }

  searchDebounce = window.setTimeout(async () => {
    try {
      const response = await fetch(`/api/chat/users/search?query=${encodeURIComponent(query)}`, {
        method: "GET",
        credentials: "include",
        headers: authHeaders()
      });

      if (!response.ok) {
        throw new Error("Search failed");
      }

      const users = await response.json();
      if (!users.length) {
        chatUserSuggestions.innerHTML = "";
        return;
      }

      chatUserSuggestions.innerHTML = users.map((user) => (
        `<button type="button" class="user-chip" data-username="${escapeHtml(user.username)}">${escapeHtml(user.username)}</button>`
      )).join("");
    } catch (error) {
      chatUserSuggestions.innerHTML = "";
    }
  }, 250);
});

chatUserSuggestions.addEventListener("click", (event) => {
  const target = event.target;
  if (!(target instanceof HTMLButtonElement)) {
    return;
  }

  const username = target.dataset.username;
  if (!username) {
    return;
  }

  chatUsernameInput.value = username;
  chatUserSuggestions.innerHTML = "";
  startConversationForm.requestSubmit();
});

conversationList.addEventListener("click", (event) => {
  const target = event.target;
  const button = target instanceof HTMLElement ? target.closest(".conversation-item") : null;
  if (!(button instanceof HTMLButtonElement)) {
    return;
  }

  const id = Number(button.dataset.id);
  const username = button.dataset.username;
  if (!id || !username) {
    return;
  }

  void loadMessages({ id, username });
  void loadConversations();
});

chatSendForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const content = (chatMessageInput.value || "").trim();

  if (!activeConversation) {
    setStatus("Select or start a conversation first.", "error");
    return;
  }

  if (!content) {
    return;
  }

  const payload = {
    receiverId: Number(activeConversation.id),
    content
  };

  try {
    if (isConnected && stompClient) {
      stompClient.send("/app/chat.send", {}, JSON.stringify(payload));
      setStatus("Message sent.", "success");
    } else {
      const response = await sendViaHttpFallback(payload);
      if (chatMessageList.querySelector(".chat-placeholder")) {
        chatMessageList.innerHTML = "";
      }
      appendMessageBubble(response, true);
      setStatus("Realtime unavailable. Message sent via fallback.", "success");
      await loadConversations();
    }

    chatMessageInput.value = "";
  } catch (error) {
    setStatus("Message delivery failed. Please retry.", "error");
  }
});

async function initChatPage() {
  setStatus("Loading chat...");

  try {
    await loadCurrentUser();
    await loadConversations();
    connectWebSocket();
    setStatus(`Signed in as ${currentUsername}.`, "success");
  } catch (error) {
    setStatus("Unable to initialize chat. Please re-login.", "error");
    setConnectionState("Not connected", "warn");
  }
}

void initChatPage();
