package com.whatsapp.controller;

import com.whatsapp.entity.Message;
import com.whatsapp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/sendMessage")
    public void sendMessage(@Payload Map<String, Object> messageData) {
        try {
            System.out.println(">>> WebSocket SEND message received: " + messageData);
            
            Object sId = messageData.get("senderId");
            Object rId = messageData.get("receiverId");
            Object msg = messageData.get("message");

            if (sId == null || rId == null) {
                System.err.println("!!! WebSocket ERROR: Missing senderId (" + sId + ") or receiverId (" + rId + ")");
                return;
            }

            Long senderId = Long.valueOf(sId.toString());
            Long receiverId = Long.valueOf(rId.toString());
            String messageText = (msg != null) ? msg.toString() : "";
            
            Object rToId = messageData.get("replyToMessageId");
            Long replyToMessageId = (rToId != null) ? Long.valueOf(rToId.toString()) : null;

            System.out.println("Processing message from " + senderId + " to " + receiverId + (replyToMessageId != null ? " replying to " + replyToMessageId : ""));

            // Save message to database
            Message savedMessage = messageService.saveMessage(senderId, receiverId, messageText, replyToMessageId);
            System.out.println("Message saved with ID: " + savedMessage.getId());

            // Send to receiver
            String receiverQueue = "/queue/messages/" + receiverId;
            System.out.println("Sending to receiver queue: " + receiverQueue);
            messagingTemplate.convertAndSend(receiverQueue, savedMessage);
            
            // Send back to sender (for sync)
            String senderQueue = "/queue/messages/" + senderId;
            System.out.println("Sending to sender queue: " + senderQueue);
            messagingTemplate.convertAndSend(senderQueue, savedMessage);
            
            System.out.println("Message broadcast complete");
            
        } catch (Exception e) {
            System.err.println("Error in sendMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/userStatus")
    @SendTo("/topic/status")
    public Map<String, Object> updateUserStatus(@Payload Map<String, Object> statusData) {
        return statusData;
    }

    @MessageMapping("/typing")
    public void handleTypingStatus(@Payload Map<String, Object> typingData) {
        try {
            Long userId = Long.valueOf(typingData.get("userId").toString());
            Long chatWithUserId = Long.valueOf(typingData.get("chatWithUserId").toString());
            Boolean isTyping = Boolean.valueOf(typingData.get("isTyping").toString());

            System.out.println("User " + userId + " typing status to " + chatWithUserId + ": " + isTyping);

            // Send typing status to the other user
            messagingTemplate.convertAndSend("/queue/typing/" + chatWithUserId, typingData);
        } catch (Exception e) {
            System.err.println("Error in handleTypingStatus: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/deleteMessage")
    public void handleDeleteMessage(@Payload Map<String, Object> deleteData) {
        try {
            Long messageId = Long.valueOf(deleteData.get("messageId").toString());
            Long userId = Long.valueOf(deleteData.get("userId").toString());
            Boolean deleteForEveryone = Boolean.valueOf(deleteData.getOrDefault("deleteForEveryone", false).toString());

            System.out.println("Deleting message " + messageId + " for user " + userId + " (deleteForEveryone: " + deleteForEveryone + ")");

            Message deletedMessage = messageService.deleteMessage(messageId, userId, deleteForEveryone);

            if (deletedMessage != null) {
                // Notify both sender and receiver about the deletion
                Map<String, Object> deleteNotification = Map.of(
                    "messageId", messageId,
                    "deleteForEveryone", deleteForEveryone,
                    "deletedBy", userId
                );

                messagingTemplate.convertAndSend("/queue/messageDeleted/" + deletedMessage.getSenderId(), deleteNotification);
                messagingTemplate.convertAndSend("/queue/messageDeleted/" + deletedMessage.getReceiverId(), deleteNotification);
            }
        } catch (Exception e) {
            System.err.println("Error in handleDeleteMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/starMessage")
    public void handleStarMessage(@Payload Map<String, Object> starData) {
        try {
            Long messageId = Long.valueOf(starData.get("messageId").toString());
            Boolean starred = Boolean.valueOf(starData.get("starred").toString());

            System.out.println("Starring message " + messageId + ": " + starred);

            Message starredMessage = messageService.starMessage(messageId, starred);

            if (starredMessage != null) {
                // Notify both sender and receiver about the star status change
                Map<String, Object> starNotification = Map.of(
                    "messageId", messageId,
                    "starred", starred
                );

                messagingTemplate.convertAndSend("/queue/messageStarred/" + starredMessage.getSenderId(), starNotification);
                messagingTemplate.convertAndSend("/queue/messageStarred/" + starredMessage.getReceiverId(), starNotification);
            }
        } catch (Exception e) {
            System.err.println("Error in handleStarMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
