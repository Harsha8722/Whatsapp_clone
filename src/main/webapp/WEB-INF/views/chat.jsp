<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
        <%@ page import="com.whatsapp.entity.User" %>
            <%@ page import="java.util.List" %>
                <!DOCTYPE html>
                <html lang="en">

                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>WhatsApp</title>
                    <link rel="stylesheet"
                        href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
                    <link rel="stylesheet" href="/css/style.css">
                    <link rel="icon" type="image/x-icon"
                        href="https://upload.wikimedia.org/wikipedia/commons/6/6b/WhatsApp.svg">
                </head>

                <body>
                    <% User currentUser=(User) request.getAttribute("currentUser"); List<User> users = (List<User>)
                            request.getAttribute("users");
                            %>
                            <div class="app-layout">
                                <!-- Nav Strip -->
                                <nav class="nav-strip">
                                    <div class="nav-top">
                                        <div class="nav-icon active" title="Chats">
                                            <i class="fas fa-comment-dots"></i>
                                        </div>
                                        <div class="nav-icon" title="Status">
                                            <i class="fas fa-circle-notch"></i>
                                        </div>
                                        <div class="nav-icon" title="Channels">
                                            <i class="fas fa-users-viewfinder"></i>
                                        </div>
                                        <div class="nav-icon" title="Communities">
                                            <i class="fas fa-users"></i>
                                        </div>
                                    </div>
                                    <div class="nav-bottom">
                                        <div class="nav-icon" title="Settings" id="settingsBtn">
                                            <i class="fas fa-cog"></i>
                                        </div>
                                        <div class="nav-icon" title="Logout" onclick="location.href='/logout'">
                                            <i class="fas fa-sign-out-alt"></i>
                                        </div>
                                        <div class="user-avatar-small" onclick="location.href='/profile'">
                                            <% if (currentUser !=null && currentUser.getProfilePhoto() !=null) { %>
                                                <img src="<%= currentUser.getProfilePhoto() %>" alt="Profile">
                                                <% } else { %>
                                                    <i class="fas fa-user"></i>
                                                    <% } %>
                                        </div>
                                    </div>
                                </nav>

                                <!-- Sidebar -->
                                <aside class="sidebar">
                                    <header class="sidebar-header">
                                        <h3>Chats</h3>
                                        <div class="sidebar-actions">
                                            <i class="fas fa-edit" title="New Chat" onclick="openNewChatModal()"></i>
                                            <i class="fas fa-ellipsis-v" title="Menu"></i>
                                        </div>
                                    </header>

                                    <div class="search-bar-container">
                                        <div class="search-bar">
                                            <i class="fas fa-search"></i>
                                            <input type="text" id="userSearch" placeholder="Search or start new chat">
                                        </div>
                                    </div>
                                    <div class="sidebar-filters">
                                        <button class="filter-btn active">All</button>
                                        <button class="filter-btn">Unread</button>
                                        <button class="filter-btn">Groups</button>
                                    </div>

                                    <div class="user-list">
                                        <c:forEach var="user" items="${users}">
                                            <c:if test="${user.id != currentUser.id}">
                                                <div class="user-item"
                                                    onclick="selectUser(this.getAttribute('data-user-id'), this.getAttribute('data-user-name'))"
                                                    id="user-${user.id}" data-user-id="${user.id}"
                                                    data-user-name="${user.name}">
                                                    <div class="user-avatar">
                                                        <c:choose>
                                                            <c:when test="${not empty user.profilePhoto}">
                                                                <img src="${user.profilePhoto}" alt="${user.name}">
                                                            </c:when>
                                                            <c:otherwise>
                                                                <div style="font-size: 20px; color: white;">
                                                                    ${user.name.substring(0,1)}</div>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                    <div class="user-details">
                                                        <div class="user-name">${user.name}</div>
                                                        <div class="user-phone">${user.phone}</div>
                                                    </div>
                                                    <div class="user-meta">
                                                        <span class="user-time" id="time-${user.id}">
                                                            ${lastMessageTimestamps[user.id]}
                                                        </span>
                                                        <span class="unread-badge" id="unread-${user.id}"
                                                            style="display:${unreadCounts[user.id] > 0 ? 'flex' : 'none'};">
                                                            ${unreadCounts[user.id]}
                                                        </span>
                                                    </div>
                                                </div>
                                                <div class="user-preview" id="preview-${user.id}" style="
                                                    padding-left: 75px;
                                                    margin-top: -15px;
                                                    font-size: 13px;
                                                    color: var(--text-secondary);
                                                    white-space: nowrap;
                                                    overflow: hidden;
                                                    text-overflow: ellipsis;
                                                    padding-bottom: 10px;
                                                    border-bottom: 1px solid var(--border-color);
                                                    margin-bottom: 5px;">
                                                    ${lastMessages[user.id]}
                                                </div>
                                            </c:if>
                                        </c:forEach>
                                    </div>
                                </aside>

                                <!-- Chat Area -->
                                <main class="chat-area" id="chatArea">
                                    <!-- No Chat Selected State -->
                                    <div id="noChatSelected" class="no-chat-selected"
                                        style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: var(--text-secondary); text-align: center; padding: 20px;">
                                        <i class="fab fa-whatsapp"
                                            style="font-size: 80px; margin-bottom: 20px; opacity: 0.1;"></i>
                                        <h2 style="color: var(--text-primary); margin-bottom: 10px; font-weight: 400;">
                                            WhatsApp Web</h2>
                                        <p style="font-size: 14px; line-height: 20px;">Send and receive messages without
                                            keeping your phone online.<br>Use WhatsApp on up to 4 linked devices and 1
                                            phone at the same time.</p>
                                        <div
                                            style="margin-top: auto; padding-bottom: 40px; font-size: 12px; display: flex; align-items: center; gap: 5px;">
                                            <i class="fas fa-lock" style="font-size: 10px;"></i> End-to-end encrypted
                                        </div>
                                    </div>

                                    <!-- Active Chat State -->
                                    <div id="activeChat" style="display: none; flex-direction: column; height: 100%;">
                                        <header class="chat-header">
                                            <div class="chat-header-info">
                                                <div class="user-avatar" id="headerAvatar"
                                                    style="width: 40px; height: 40px; margin-right: 15px;">
                                                    <i class="fas fa-user"></i>
                                                </div>
                                                <div class="chat-user-details">
                                                    <span class="chat-user-name" id="selectedUserName">User Name</span>
                                                    <span class="user-status-text"
                                                        id="selectedUserStatus">offline</span>
                                                </div>
                                            </div>
                                            <div class="chat-header-actions">
                                                <i class="fas fa-search"></i>
                                                <i class="fas fa-ellipsis-v"></i>
                                            </div>
                                        </header>

                                        <div class="chat-messages" id="chatMessages">
                                            <!-- Messages will be appended here -->
                                        </div>

                                        <div id="replyPreviewContainer"
                                            style="display: none; background: #1e2a30; border-left: 4px solid #00a884; padding: 10px 15px; margin: 0 10px 5px 10px; border-radius: 8px; position: relative;">
                                            <div
                                                style="font-size: 13px; color: #00a884; font-weight: bold; margin-bottom: 3px;">
                                                Replying to</div>
                                            <div id="replyPreviewText"
                                                style="font-size: 14px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 90%;">
                                                Message preview...</div>
                                            <i class="fas fa-times" id="cancelReplyBtn"
                                                style="position: absolute; right: 15px; top: 15px; cursor: pointer; color: var(--text-secondary); font-size: 14px;"></i>
                                            <input type="hidden" id="replyToMessageId" value="">
                                        </div>
                                        <div class="chat-input-container">
                                            <button class="icon-btn" id="emojiBtn"><i class="far fa-smile"></i></button>
                                            <button class="icon-btn"><i class="fas fa-plus"></i></button>
                                            <div class="input-wrapper">
                                                <input type="text" id="messageInput" placeholder="Type a message"
                                                    autocomplete="off">
                                            </div>
                                            <button class="icon-btn" id="sendBtn" onclick="sendMessage()"><i
                                                    class="fas fa-microphone"></i></button>
                                        </div>
                                    </div>
                                </main>
                            </div>

                            <!-- User Context Data -->
                            <div id="user-context" data-current-user-id="<c:out value='${currentUser.id}'/>"
                                data-current-user-name="<c:out value='${currentUser.name}'/>" style="display:none;">
                            </div>

                            <!-- Hidden File Input for Media -->
                            <input type="file" id="mediaInput" style="display:none;"
                                accept="image/*,video/*,audio/*,.pdf,.doc,.docx,.txt">

                            <!-- Scripts -->
                            <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
                            <script
                                src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.5.1/sockjs.min.js"></script>
                            <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
                            <script
                                src="https://cdn.jsdelivr.net/npm/@joeattardi/emoji-button@4.6.4/dist/index.min.js"></script>
                            <script src="/js/chat.js"></script>
                </body>

                </html>