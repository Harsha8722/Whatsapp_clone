package com.whatsapp.service;

import com.whatsapp.entity.Message;
import com.whatsapp.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    public Message saveMessage(Long senderId, Long receiverId, String messageText) {
        Message message = new Message(senderId, receiverId, messageText);
        return messageRepository.save(message);
    }

    public Message saveMessage(Long senderId, Long receiverId, String messageText, Long replyToMessageId) {
        Message message = new Message(senderId, receiverId, messageText, replyToMessageId);
        return messageRepository.save(message);
    }

    public Message saveMediaMessage(Long senderId, Long receiverId, String messageText, String messageType, String mediaUrl, Long replyToMessageId) {
        Message message = new Message(senderId, receiverId, messageText, messageType, mediaUrl);
        message.setReplyToMessageId(replyToMessageId);
        return messageRepository.save(message);
    }

    public void markAsRead(Long senderId, Long receiverId) {
        List<Message> unread = messageRepository.findChatMessages(senderId, receiverId);
        unread.stream()
              .filter(m -> m.getReceiverId().equals(receiverId) && !m.isRead())
              .forEach(m -> {
                  m.setRead(true);
                  messageRepository.save(m);
              });
    }

    public long countUnread(Long senderId, Long receiverId) {
        return messageRepository.countUnreadMessages(senderId, receiverId);
    }


    public List<Message> getChatMessages(Long userId1, Long userId2) {
        List<Message> messages = messageRepository.findChatMessages(userId1, userId2);
        // Filter out deleted messages based on who is viewing
        return messages.stream()
                .filter(m -> {
                    if (m.getSenderId().equals(userId1)) {
                        return !m.isDeletedBySender();
                    } else {
                        return !m.isDeletedByReceiver();
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public Message getLastMessage(Long userId1, Long userId2) {
        List<Message> messages = getChatMessages(userId1, userId2);
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    public Message deleteMessage(Long messageId, Long userId, boolean deleteForEveryone) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) return null;

        if (deleteForEveryone && message.getSenderId().equals(userId)) {
            // Delete for everyone - mark as deleted for both
            message.setDeletedBySender(true);
            message.setDeletedByReceiver(true);
        } else {
            // Delete for me only
            if (message.getSenderId().equals(userId)) {
                message.setDeletedBySender(true);
            } else {
                message.setDeletedByReceiver(true);
            }
        }
        return messageRepository.save(message);
    }

    public List<Message> searchMessages(Long userId, String query) {
        List<Message> allMessages = messageRepository.findAll();
        return allMessages.stream()
                .filter(m -> (m.getSenderId().equals(userId) || m.getReceiverId().equals(userId)))
                .filter(m -> m.getMessage() != null && m.getMessage().toLowerCase().contains(query.toLowerCase()))
                .filter(m -> {
                    if (m.getSenderId().equals(userId)) {
                        return !m.isDeletedBySender();
                    } else {
                        return !m.isDeletedByReceiver();
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public Message markAsDelivered(Long messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message != null && message.getDeliveredAt() == null) {
            message.setDeliveredAt(java.time.LocalDateTime.now());
            return messageRepository.save(message);
        }
        return message;
    }

    public Message markAsReadWithTimestamp(Long messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message != null && !message.isRead()) {
            message.setRead(true);
            message.setReadAt(java.time.LocalDateTime.now());
            return messageRepository.save(message);
        }
        return message;
    }

    public Message starMessage(Long messageId, boolean starred) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message != null) {
            message.setStarred(starred);
            return messageRepository.save(message);
        }
        return null;
    }
}
