let stompClient = null;
let selectedUserId = null;
let selectedUserName = null;
let typingTimeout = null;

// User context from hidden element
const userCtx = document.getElementById('user-context');
const currentUserId = userCtx ? userCtx.getAttribute('data-current-user-id') : null;
const currentUserName = userCtx ? userCtx.getAttribute('data-current-user-name') : null;

// Connect on load
document.addEventListener('DOMContentLoaded', function () {
    if (!currentUserId) {
        console.error('CRITICAL: currentUserId is null.');
        return;
    }
    connectWebSocket();
    setupEventListeners();
    setupEmojiPicker();
});

function setupEmojiPicker() {
    const button = document.querySelector('#emojiBtn');
    const picker = new EmojiButton({
        position: 'top-start',
        theme: 'dark',
        autoHide: false
    });

    picker.on('emoji', emoji => {
        const input = document.getElementById('messageInput');
        input.value += emoji;
        input.focus();
    });

    button.addEventListener('click', () => {
        picker.togglePicker(button);
    });
}

function connectWebSocket() {
    const socket = new SockJS('/chat');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected to WhatsApp WebSocket: ' + frame);

        // Subscribe to my own queue
        stompClient.subscribe('/queue/messages/' + currentUserId, function (msg) {
            console.log("Received message raw body: ", msg.body);
            try {
                const parsedMsg = JSON.parse(msg.body);
                console.log("Parsed message: ", parsedMsg);
                handleIncomingMessage(parsedMsg);
            } catch (e) {
                console.error("Error parsing message: ", e);
            }
        });

        stompClient.subscribe('/topic/status', function (msg) {
            const data = JSON.parse(msg.body);
            updateStatusInUI(data.userId, data.status);
        });

        stompClient.subscribe('/queue/typing/' + currentUserId, function (msg) {
            handleTypingIndicator(JSON.parse(msg.body));
        });

        stompClient.subscribe('/queue/messageDeleted/' + currentUserId, function (msg) {
            console.log("Received delete notification", msg.body);
            const data = JSON.parse(msg.body);
            // Handle deletion in UI
            const msgElement = document.getElementById('msg-' + data.messageId);
            if (msgElement) msgElement.remove();
        });


        stompClient.subscribe('/queue/messageStarred/' + currentUserId, function (msg) {
            console.log("Received star notification", msg.body);
            const data = JSON.parse(msg.body);
            handleStarNotification(data);
        });

        // Online status
        stompClient.send("/app/userStatus", {}, JSON.stringify({ userId: currentUserId, status: 'online' }));
    }, function (err) {
        console.error("WebSocket Connection Error: ", err);
        setTimeout(connectWebSocket, 5000);
    });
}

function setupEventListeners() {
    const messageInput = document.getElementById('messageInput');
    if (messageInput) {
        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                sendMessage();
                sendTypingStatus(false);
            }
        });

        messageInput.addEventListener('input', () => {
            sendTypingStatus(true);
            clearTimeout(typingTimeout);
            typingTimeout = setTimeout(() => sendTypingStatus(false), 3000);
        });
    }

    // Search bar filtering
    const userSearch = document.getElementById('userSearch');
    if (userSearch) {
        userSearch.addEventListener('input', function (e) {
            const term = e.target.value.toLowerCase();
            document.querySelectorAll('.user-item').forEach(item => {
                const name = item.getAttribute('data-user-name').toLowerCase();
                item.style.display = name.includes(term) ? 'flex' : 'none';
            });
        });
    }

    // Media upload trigger
    const attachmentBtn = document.querySelector('.chat-header-actions i.fa-plus') || document.querySelector('.fa-plus')?.parentElement;
    const mediaInput = document.getElementById('mediaInput');
    if (attachmentBtn && mediaInput) {
        attachmentBtn.addEventListener('click', () => mediaInput.click());
        mediaInput.addEventListener('change', handleMediaUpload);
    }

    // Cancel reply button
    const cancelReplyBtn = document.getElementById('cancelReplyBtn');
    if (cancelReplyBtn) {
        cancelReplyBtn.addEventListener('click', clearReply);
    }
}

function selectUser(id, name) {
    if (selectedUserId == id) return;
    selectedUserId = id;
    selectedUserName = name;

    document.querySelectorAll('.user-item').forEach(item => item.classList.remove('active'));
    const selectedItem = document.getElementById('user-' + id);
    if (selectedItem) {
        selectedItem.classList.add('active');
        const badge = document.getElementById('unread-' + id);
        if (badge) { badge.style.display = 'none'; badge.textContent = '0'; }
    }

    document.getElementById('noChatSelected').style.display = 'none';
    document.getElementById('activeChat').style.display = 'flex';
    document.getElementById('selectedUserName').textContent = name;

    // Header Avatar
    const headerAvatar = document.getElementById('headerAvatar');
    headerAvatar.innerHTML = `<div style="width:100%; height:100%; display:flex; align-items:center; justify-content:center; background:#2a2f32; color:white; font-weight:bold; font-size:18px;">${name.charAt(0)}</div>`;

    loadHistory(id);
    document.getElementById('messageInput').focus();

    // Check if it's self-chat
    if (String(id) === String(currentUserId)) {
        document.getElementById('selectedUserName').textContent = name + " (You)";
    }
}

function loadHistory(id) {
    const container = document.getElementById('chatMessages');
    container.innerHTML = '<div class="chat-placeholder" style="text-align:center; padding: 20px; opacity:0.5;">Loading...</div>';

    console.log("Fetching history for user: " + id);
    fetch('/api/messages/' + id)
        .then(res => res.json())
        .then(messages => {
            console.log("History fetched:", messages);
            container.innerHTML = '';
            if (messages && messages.length > 0) {
                messages.forEach(msg => {
                    console.log("Appending message:", msg);
                    appendMessage(msg);
                });
                scrollToBottom(true);
            } else {
                console.log("No messages found.");
                container.innerHTML = '<div class="chat-placeholder no-messages" style="text-align:center; padding: 20px; opacity:0.3;">No messages here.</div>';
            }
        })
        .catch(err => {
            console.error("Error loading history:", err);
            container.innerHTML = '<div style="text-align:center; padding: 20px; color:red;">Error loading messages.</div>';
        });
}

function sendMessage() {
    const input = document.getElementById('messageInput');
    const text = input.value.trim();
    if (!text || !selectedUserId || !stompClient) return;

    const replyToId = document.getElementById('replyToMessageId').value;
    const msgObj = {
        senderId: currentUserId,
        receiverId: selectedUserId,
        message: text,
        timestamp: new Date().toISOString(),
        replyToMessageId: replyToId ? parseInt(replyToId) : null
    };
    stompClient.send("/app/sendMessage", {}, JSON.stringify(msgObj));

    // Optimistic update for sidebar
    updateSidebarPreview(selectedUserId, text, new Date());

    input.value = '';
    clearReply();
}

function handleIncomingMessage(msg) {
    const senderId = msg.senderId || msg.sender_id;
    const receiverId = msg.receiverId || msg.receiver_id;
    const text = msg.message || msg.text || '';
    const timestamp = msg.timestamp ? new Date(msg.timestamp) : new Date();

    if (selectedUserId && (String(senderId) === String(selectedUserId) || String(receiverId) === String(selectedUserId))) {
        appendMessage(msg);
        scrollToBottom(false);
    } else if (String(receiverId) === String(currentUserId)) {
        updateUnreadBadge(senderId);
    }

    // Update sidebar for the other person
    if (String(senderId) !== String(currentUserId)) {
        updateSidebarPreview(senderId, text, timestamp);
    }
}

function updateSidebarPreview(userId, message, dateObj) {
    const previewEl = document.getElementById('preview-' + userId);
    if (previewEl) {
        if (message.length > 30) message = message.substring(0, 27) + "...";
        previewEl.textContent = message;
    }

    const timeEl = document.getElementById('time-' + userId);
    if (timeEl) {
        const hours = dateObj.getHours().toString().padStart(2, '0');
        const minutes = dateObj.getMinutes().toString().padStart(2, '0');
        timeEl.textContent = hours + ':' + minutes;
    }

    // Move this user to top (optional, but good UX)
    const userItem = document.getElementById('user-' + userId);
    if (userItem) {
        const parent = userItem.parentNode;
        parent.insertBefore(userItem, parent.firstChild);
    }
}

function appendMessage(msg) {
    const container = document.getElementById('chatMessages');

    // Clear placeholders if they exist
    const placeholder = container.querySelector('.chat-placeholder');
    if (placeholder) {
        container.innerHTML = '';
    }

    const senderId = msg.senderId || msg.sender_id;
    const isSent = String(senderId) === String(currentUserId);
    const text = msg.message || msg.text || '';
    const messageId = msg.id;
    const isStarred = msg.isStarred || msg.starred || false;

    let ts = '';
    try {
        const d = new Date(msg.timestamp);
        ts = d.getHours().toString().padStart(2, '0') + ':' + d.getMinutes().toString().padStart(2, '0');
    } catch (e) {
        console.error("Timestamp parse error:", e);
    }

    // Check if message already exists
    if (messageId && document.getElementById('msg-' + messageId)) {
        console.log("Message already exists in UI:", messageId);
        // Maybe update star status
        const starIcon = document.getElementById('msg-' + messageId).querySelector('.star-icon');
        if (starIcon) starIcon.style.display = isStarred ? 'inline' : 'none';
        return;
    }

    const div = document.createElement('div');
    div.className = 'message ' + (isSent ? 'sent' : 'received');
    if (messageId) div.id = 'msg-' + messageId;

    let replyHtml = '';
    const replyToId = msg.replyToMessageId;
    if (replyToId) {
        const parentMsg = document.getElementById('msg-' + replyToId);
        let parentText = "Message content unavailable";
        let parentSender = "User";

        if (parentMsg) {
            parentText = parentMsg.querySelector('.message-text').textContent;
            parentSender = parentMsg.classList.contains('sent') ? 'You' : selectedUserName;
        }

        replyHtml = `
            <div class="message-reply-context" onclick="scrollToMessage(${replyToId})">
                <div class="reply-sender">${parentSender}</div>
                <div class="reply-text">${escapeHtml(parentText)}</div>
            </div>
        `;
    }

    div.innerHTML = `
        <div class="message-bubble" oncontextmenu="showContextMenu(event, ${messageId})">
            ${replyHtml}
            <div class="message-text">${escapeHtml(text)}</div>
            <div class="message-footer">
                <i class="fas fa-star star-icon" style="display: ${isStarred ? 'inline' : 'none'}; color: #ffbc00; font-size: 10px; margin-right: 5px;"></i>
                <div class="message-time">
                    ${ts} ${isSent ? '<i class="fas fa-check-double" style="font-size:10px; margin-left:2px;"></i>' : ''}
                </div>
            </div>
        </div>
    `;
    container.appendChild(div);
}

function updateUnreadBadge(userId) {
    const badge = document.getElementById('unread-' + userId);
    if (badge) {
        badge.textContent = (parseInt(badge.textContent) || 0) + 1;
        badge.style.display = 'flex';
    }
}

function updateStatusInUI(userId, status) {
    if (selectedUserId == userId) {
        const el = document.getElementById('selectedUserStatus');
        if (el) el.textContent = status === 'online' ? 'online' : 'offline';
    }
}

function sendTypingStatus(isTyping) {
    if (stompClient && selectedUserId) {
        stompClient.send("/app/typing", {}, JSON.stringify({ userId: currentUserId, chatWithUserId: selectedUserId, isTyping }));
    }
}

function handleTypingIndicator(data) {
    if (data.userId == selectedUserId) {
        const el = document.getElementById('selectedUserStatus');
        if (el) el.textContent = data.isTyping ? 'typing...' : 'online';
    }
}

function scrollToBottom(instant = false) {
    const c = document.getElementById('chatMessages');
    if (instant) {
        c.scrollTop = c.scrollHeight;
    } else {
        c.scrollTo({
            top: c.scrollHeight,
            behavior: 'smooth'
        });
    }
}

function handleMediaUpload(event) {
    const file = event.target.files[0];
    if (!file || !selectedUserId) return;

    const formData = new FormData();
    formData.append('file', file);
    formData.append('receiverId', selectedUserId);

    let type = 'IMAGE';
    if (file.type.startsWith('video/')) type = 'VIDEO';
    else if (file.type.startsWith('audio/')) type = 'AUDIO';
    else if (!file.type.startsWith('image/')) type = 'DOCUMENT';

    formData.append('messageType', type);

    console.log("Uploading media...", type);

    fetch('/api/messages/media', {
        method: 'POST',
        body: formData
    })
        .then(res => res.json())
        .then(msg => {
            console.log("Media uploaded:", msg);
            // Dispatch via WebSocket so other person gets it
            stompClient.send("/app/sendMessage", {}, JSON.stringify(msg));
            appendMessage(msg);
            scrollToBottom(false);
        })
        .catch(err => console.error("Media upload error:", err));
}

function handleStarNotification(data) {
    const msgElement = document.getElementById('msg-' + data.messageId);
    if (msgElement) {
        const starIcon = msgElement.querySelector('.star-icon');
        if (starIcon) {
            starIcon.style.display = data.starred ? 'inline' : 'none';
        }
    }
}

function showContextMenu(e, messageId) {
    e.preventDefault();
    if (!messageId) return;

    const menu = document.createElement('div');
    menu.className = 'custom-context-menu';
    menu.style.position = 'fixed';
    menu.style.top = e.clientY + 'px';
    menu.style.left = e.clientX + 'px';
    menu.style.background = '#233138';
    menu.style.color = 'white';
    menu.style.padding = '8px 0';
    menu.style.borderRadius = '5px';
    menu.style.boxShadow = '0 2px 5px rgba(0,0,0,0.3)';
    menu.style.zIndex = '1000';
    menu.style.fontSize = '14px';
    menu.style.minWidth = '120px';

    const replyBtn = document.createElement('div');
    replyBtn.style.padding = '8px 15px';
    replyBtn.style.cursor = 'pointer';
    replyBtn.innerHTML = '<i class="fas fa-reply" style="margin-right:10px;"></i> Reply';
    replyBtn.onclick = () => {
        setReplyTo(messageId);
        document.body.removeChild(menu);
    };
    replyBtn.onmouseover = () => replyBtn.style.background = '#182229';
    replyBtn.onmouseout = () => replyBtn.style.background = 'transparent';

    const starBtn = document.createElement('div');
    starBtn.style.padding = '8px 15px';
    starBtn.style.cursor = 'pointer';
    starBtn.innerHTML = '<i class="fas fa-star" style="margin-right:10px;"></i> Star/Unstar';
    // ... rest remains same but including the additions
    menu.appendChild(replyBtn);
    menu.appendChild(starBtn);
    menu.appendChild(deleteBtn);
    document.body.appendChild(menu);

    const closeMenu = () => {
        if (document.body.contains(menu)) document.body.removeChild(menu);
        document.removeEventListener('click', closeMenu);
    };
    setTimeout(() => document.addEventListener('click', closeMenu), 10);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function openNewChatModal() { /* TBD or handled via sidebar search */ }

function setReplyTo(messageId) {
    const msgEl = document.getElementById('msg-' + messageId);
    if (!msgEl) return;

    const text = msgEl.querySelector('.message-text').textContent;
    document.getElementById('replyToMessageId').value = messageId;
    document.getElementById('replyPreviewText').textContent = text;
    document.getElementById('replyPreviewContainer').style.display = 'block';
    document.getElementById('messageInput').focus();
}

function clearReply() {
    document.getElementById('replyToMessageId').value = '';
    document.getElementById('replyPreviewContainer').style.display = 'none';
}

function scrollToMessage(messageId) {
    const el = document.getElementById('msg-' + messageId);
    if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        el.querySelector('.message-bubble').style.backgroundColor = 'rgba(0, 168, 132, 0.3)';
        setTimeout(() => {
            el.querySelector('.message-bubble').style.backgroundColor = '';
        }, 2000);
    }
}

window.onbeforeunload = () => {
    if (stompClient) stompClient.send("/app/userStatus", {}, JSON.stringify({ userId: currentUserId, status: 'offline' }));
};
