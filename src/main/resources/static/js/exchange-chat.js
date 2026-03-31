/**
 * Exchange-based Chat System
 * Real-time messaging tied to specific book exchanges
 * Supports WebSocket (primary) with HTTP fallback
 */

(function() {
    'use strict';

    // ============ State Management ============
    const state = {
        stompClient: null,
        isConnected: false,
        activeExchangeId: null,
        currentUserId: null,
        exchanges: [],
        messages: {},
        subscriptions: {}
    };

    // ============ DOM Elements ============
    const DOM = {
        exchangeListContainer: document.getElementById('exchangeListContainer'),
        exchangeCount: document.getElementById('exchangeCount'),
        messagesArea: document.getElementById('messagesArea'),
        chatContainer: document.getElementById('chatContainer'),
        noChatSelected: document.getElementById('noChatSelected'),
        messageSendForm: document.getElementById('messageSendForm'),
        messageInput: document.getElementById('messageInput'),
        charCountCurrent: document.getElementById('charCountCurrent'),
        charCountMax: document.getElementById('charCountMax'),
        chatPartnerName: document.getElementById('chatPartnerName'),
        bookBeingRequested: document.getElementById('bookBeingRequested'),
        bookOffered: document.getElementById('bookOffered'),
        exchangeStatus: document.getElementById('exchangeStatus')
    };

    // ============ Initialization ============
    document.addEventListener('DOMContentLoaded', function() {
        void init();
    });

    async function init() {
        if (!DOM.exchangeListContainer || !DOM.messageSendForm || !DOM.messageInput || !DOM.messagesArea) {
            return;
        }

        await loadCurrentUser();
        await loadExchanges();
        connectWebSocket();
        setupEventListeners();
    }

    // ============ Event Listeners ============
    function setupEventListeners() {
        // Message form submission
        DOM.messageSendForm.addEventListener('submit', handleMessageSubmit);

        // Character counter
        DOM.messageInput.addEventListener('input', function() {
            const length = this.value.length;
            DOM.charCountCurrent.textContent = length;
            DOM.charCountCurrent.style.color = length > 1000 ? '#e74c3c' : '#999';
        });

        // Auto-scroll messages when new ones arrive
        const observer = new MutationObserver(() => {
            DOM.messagesArea.scrollTop = DOM.messagesArea.scrollHeight;
        });

        observer.observe(DOM.messagesArea, { childList: true });
    }

    // ============ Exchange Loading ============
    function loadExchanges() {
        return fetch('/api/exchange/my-chats', {
            credentials: 'include',
            headers: withAuthHeaders({
                'Accept': 'application/json'
            })
        })
        .then(response => {
            if (!response.ok) {
                console.error('Failed to load exchanges:', response.status);
                return [];
            }
            return response.json();
        })
        .then(exchanges => {
            state.exchanges = exchanges || [];
            renderExchangeList();
        })
        .catch(error => console.error('Error loading exchanges:', error));
    }

    function renderExchangeList() {
        DOM.exchangeCount.textContent = state.exchanges.length;

        if (state.exchanges.length === 0) {
            DOM.exchangeListContainer.innerHTML = `
                <div class="empty-state">
                    <p>No active exchanges</p>
                    <small>Once you have pending or approved exchanges, they'll appear here</small>
                </div>
            `;
            return;
        }

        DOM.exchangeListContainer.innerHTML = state.exchanges.map(exchange => `
            <div class="exchange-item ${exchange.exchangeRequestId === state.activeExchangeId ? 'active' : ''}"
                 data-exchange-id="${exchange.exchangeRequestId}">
                <div class="exchange-item-avatar">${exchange.otherUserUsername.charAt(0).toUpperCase()}</div>
                <div class="exchange-item-content">
                    <div class="exchange-item-header">
                        <span class="exchange-item-user">${escapeHtml(exchange.otherUserUsername)}</span>
                        ${exchange.lastMessageAt ? `<span class="exchange-item-time">${formatTime(exchange.lastMessageAt)}</span>` : ''}
                    </div>
                    <div class="exchange-item-books">
                        ${escapeHtml(exchange.bookTitle)} ↔ ${escapeHtml(exchange.offeredBookTitle)}
                    </div>
                    ${exchange.lastMessageContent ? `
                        <div class="exchange-item-message">
                            <strong>${escapeHtml(exchange.lastMessageSenderUsername || 'User')}:</strong> ${escapeHtml(exchange.lastMessageContent.substring(0, 50))}${exchange.lastMessageContent.length > 50 ? '...' : ''}
                        </div>
                    ` : ''}
                </div>
            </div>
        `).join('');

        // Add click handlers
        document.querySelectorAll('.exchange-item').forEach(item => {
            item.addEventListener('click', function() {
                selectExchange(parseInt(this.dataset.exchangeId));
            });
        });
    }

    // ============ Exchange Selection ============
    function selectExchange(exchangeId) {
        const previousExchangeId = state.activeExchangeId;

        if (previousExchangeId && state.subscriptions[previousExchangeId]) {
            state.subscriptions[previousExchangeId].unsubscribe();
            delete state.subscriptions[previousExchangeId];
        }

        state.activeExchangeId = exchangeId;

        // Update UI
        document.querySelectorAll('.exchange-item').forEach(item => {
            item.classList.toggle('active', parseInt(item.dataset.exchangeId) === exchangeId);
        });

        // Load messages
        loadMessages(exchangeId);

        // Update chat header
        const exchange = state.exchanges.find(e => e.exchangeRequestId === exchangeId);
        if (exchange) {
            DOM.chatPartnerName.textContent = escapeHtml(exchange.otherUserUsername);
            DOM.bookBeingRequested.textContent = escapeHtml(exchange.bookTitle);
            DOM.bookOffered.textContent = escapeHtml(exchange.offeredBookTitle);

            const statusClass = exchange.exchangeStatus.toLowerCase();
            DOM.exchangeStatus.textContent = exchange.exchangeStatus;
            DOM.exchangeStatus.className = 'status-badge ' + statusClass;
        }

        // Show chat container
        DOM.noChatSelected.style.display = 'none';
        DOM.chatContainer.style.display = 'flex';

        // Subscribe to new messages via WebSocket
        subscribeToExchangeMessages(exchangeId);

        // Clear input
        DOM.messageInput.value = '';
        DOM.charCountCurrent.textContent = '0';
    }

    // ============ Message Loading ============
    function loadMessages(exchangeId) {
        DOM.messagesArea.innerHTML = '<div style="text-align: center; color: #999;">Loading messages...</div>';

        fetch(`/api/exchange/${exchangeId}/messages`, {
            credentials: 'include',
            headers: withAuthHeaders({
                'Accept': 'application/json'
            })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to load messages');
            }
            return response.json();
        })
        .then(messages => {
            state.messages[exchangeId] = messages || [];
            renderMessages();
        })
        .catch(error => {
            console.error('Error loading messages:', error);
            DOM.messagesArea.innerHTML = '<div style="text-align: center; color: #e74c3c;">Failed to load messages</div>';
        });
    }

    function renderMessages() {
        const messages = state.messages[state.activeExchangeId] || [];

        if (messages.length === 0) {
            DOM.messagesArea.innerHTML = '<div style="text-align: center; color: #999; padding: 2rem;">No messages yet. Start the conversation!</div>';
            return;
        }

        DOM.messagesArea.innerHTML = messages.map(msg => {
            const isSent = Number(msg.senderId) === Number(state.currentUserId);
            const time = new Date(msg.timestamp);
            const timeStr = time.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

            return `
                <div class="message-bubble ${isSent ? 'sent' : 'received'}">
                    <div>
                        ${!isSent ? `<div class="message-sender">${escapeHtml(msg.senderUsername)}</div>` : ''}
                        <div class="message-bubble-content">
                            ${escapeHtml(msg.content)}
                        </div>
                        <div class="message-time">${timeStr}</div>
                    </div>
                </div>
            `;
        }).join('');

        // Auto scroll to bottom
        DOM.messagesArea.scrollTop = DOM.messagesArea.scrollHeight;
    }

    // ============ Message Sending ============
    function handleMessageSubmit(e) {
        e.preventDefault();

        const content = DOM.messageInput.value.trim();
        if (!content) return;
        if (content.length > 1200) {
            alert('Message is too long (max 1200 characters)');
            return;
        }

        sendMessage(content);
    }

    function sendMessage(content) {
        if (!state.activeExchangeId) return;

        const exchangeId = state.activeExchangeId;
        const payload = { content: content };

        // Persist through HTTP to avoid "message not sent" when websocket send fails silently.
        sendViaHttp(exchangeId, payload);
    }

    function sendViaHttp(exchangeId, payload) {
        return fetch(`/api/exchange/${exchangeId}/messages`, {
            method: 'POST',
            credentials: 'include',
            headers: withAuthHeaders({
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }),
            body: JSON.stringify(payload)
        })
        .then(response => {
            if ((response.status === 401 || response.status === 403) && getStoredToken()) {
                return fetch(`/api/exchange/${exchangeId}/messages`, {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });
            }
            return response;
        })
        .then(response => {
            const contentType = response.headers.get('content-type') || '';
            if (response.ok && !contentType.includes('application/json')) {
                throw new Error('Authentication/session issue detected. Please sign in again.');
            }
            return response;
        })
        .then(response => {
            if (!response.ok) {
                return response.json()
                    .then(errorBody => {
                        throw new Error(errorBody && errorBody.message ? errorBody.message : 'Unable to send message');
                    })
                    .catch(() => {
                        throw new Error('Unable to send message');
                    });
            }
            return response.json();
        })
        .then(response => {
            // Add message to local state and render
            if (!state.messages[state.activeExchangeId]) {
                state.messages[state.activeExchangeId] = [];
            }
            const exists = state.messages[state.activeExchangeId].some(msg => Number(msg.id) === Number(response.id));
            if (!exists) {
                state.messages[state.activeExchangeId].push(response);
            }
            renderMessages();

            DOM.messageInput.value = '';
            DOM.charCountCurrent.textContent = '0';
        })
        .catch(error => {
            console.error('Error sending message:', error);
            alert(error && error.message ? error.message : 'Unable to send message right now. Please try again.');
        });
    }

    // ============ WebSocket Connection ============
    function connectWebSocket() {
        try {
            const token = getStoredToken();

            const socket = new SockJS('/ws-chat');
            state.stompClient = Stomp.over(socket);
            state.stompClient.debug = function() {};

            // Get token for authentication
            const authHeaders = token ? { 'Authorization': 'Bearer ' + token } : {};

            state.stompClient.connect(authHeaders, function(frame) {
                state.isConnected = true;
                console.log('WebSocket connected');

                // Re-subscribe to active exchange if one is selected
                if (state.activeExchangeId) {
                    subscribeToExchangeMessages(state.activeExchangeId);
                }
            }, function(error) {
                console.error('WebSocket connection error:', error);
                state.isConnected = false;
            });

        } catch (error) {
            console.error('Error setting up WebSocket:', error);
            state.isConnected = false;
        }
    }

    function subscribeToExchangeMessages(exchangeId) {
        if (!state.stompClient || !state.isConnected) {
            console.warn('WebSocket not connected, using HTTP polling');
            return;
        }

        // Unsubscribe from previous subscription
        if (state.subscriptions[exchangeId]) {
            state.subscriptions[exchangeId].unsubscribe();
        }

        // Subscribe to exchange topic
        state.subscriptions[exchangeId] = state.stompClient.subscribe(
            `/topic/chat/${exchangeId}`,
            function(message) {
                const messageData = JSON.parse(message.body);

                // Add to messages
                if (!state.messages[exchangeId]) {
                    state.messages[exchangeId] = [];
                }
                const alreadyExists = state.messages[exchangeId].some(msg => Number(msg.id) === Number(messageData.id));
                if (!alreadyExists) {
                    state.messages[exchangeId].push(messageData);
                }

                // Re-render only if still on this exchange
                if (state.activeExchangeId === exchangeId) {
                    renderMessages();
                }
            }
        );

        // Backward-compatible fallback topic for older backend versions.
        const legacyKey = `legacy-${exchangeId}`;
        if (state.subscriptions[legacyKey]) {
            state.subscriptions[legacyKey].unsubscribe();
        }
        state.subscriptions[legacyKey] = state.stompClient.subscribe(
            `/topic/exchange/${exchangeId}`,
            function(message) {
                const messageData = JSON.parse(message.body);
                if (!state.messages[exchangeId]) {
                    state.messages[exchangeId] = [];
                }
                const alreadyExists = state.messages[exchangeId].some(msg => Number(msg.id) === Number(messageData.id));
                if (!alreadyExists) {
                    state.messages[exchangeId].push(messageData);
                }
                if (state.activeExchangeId === exchangeId) {
                    renderMessages();
                }
            }
        );
    }

    // ============ Utility Functions ============
    function withAuthHeaders(extraHeaders) {
        const token = getStoredToken();
        if (!token) {
            return extraHeaders || {};
        }

        return {
            ...(extraHeaders || {}),
            'Authorization': 'Bearer ' + token
        };
    }

    function loadCurrentUser() {
        return fetch('/api/user/profile', {
            method: 'GET',
            credentials: 'include',
            headers: withAuthHeaders({
                'Accept': 'application/json'
            })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to load current user profile');
            }
            return response.json();
        })
        .then(profile => {
            state.currentUserId = Number(profile.id);
        })
        .catch(error => {
            console.error('Unable to resolve current user:', error);
        });
    }

    function getStoredToken() {
        const tokens = ['jwtToken', 'token', 'authToken'];
        for (let t of tokens) {
            const token = localStorage.getItem(t);
            if (token) return token;
        }
        return null;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatTime(isoString) {
        const date = new Date(isoString);
        if (Number.isNaN(date.getTime())) {
            return '';
        }

        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMins / 60);
        const diffDays = Math.floor(diffHours / 24);

        if (diffMins < 1) return 'Now';
        if (diffMins < 60) return diffMins + 'm ago';
        if (diffHours < 24) return diffHours + 'h ago';
        if (diffDays < 7) return diffDays + 'd ago';

        return date.toLocaleDateString();
    }

})();
