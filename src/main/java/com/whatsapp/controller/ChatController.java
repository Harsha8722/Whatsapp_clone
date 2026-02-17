package com.whatsapp.controller;

import com.whatsapp.entity.Message;
import com.whatsapp.entity.User;
import com.whatsapp.service.MessageService;
import com.whatsapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class ChatController {

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @GetMapping("/chat")
    public String showChatPage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Refresh user data from database to get updated status
        currentUser = userService.getUserById(currentUser.getId());
        session.setAttribute("user", currentUser);

        List<User> allUsers = userService.getAllUsers();
        
        // Get unread counts for each user
        Map<Long, Long> unreadCounts = new HashMap<>();
        for (User user : allUsers) {
            if (!user.getId().equals(currentUser.getId())) {
                unreadCounts.put(user.getId(), messageService.countUnread(user.getId(), currentUser.getId()));
            }
        }
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", allUsers);
        model.addAttribute("unreadCounts", unreadCounts);

        // Get last messages for preview
        Map<Long, String> lastMessages = new HashMap<>();
        Map<Long, String> lastMessageTimestamps = new HashMap<>();

        for (User user : allUsers) {
            if (!user.getId().equals(currentUser.getId())) {
                Message lastMsg = messageService.getLastMessage(currentUser.getId(), user.getId());
                if (lastMsg != null) {
                    if ("IMAGE".equals(lastMsg.getMessageType())) {
                        lastMessages.put(user.getId(), "📷 Photo");
                    } else if ("VIDEO".equals(lastMsg.getMessageType())) {
                        lastMessages.put(user.getId(), "🎥 Video");
                    } else {
                        String content = lastMsg.getMessage();
                        if (content != null && content.length() > 30) {
                            content = content.substring(0, 27) + "...";
                        }
                        lastMessages.put(user.getId(), content);
                    }
                    
                    // Format timestamp
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
                    lastMessageTimestamps.put(user.getId(), lastMsg.getTimestamp().format(formatter));
                }
            }
        }
        model.addAttribute("lastMessages", lastMessages);
        model.addAttribute("lastMessageTimestamps", lastMessageTimestamps);
        
        return "chat";
    }

    @GetMapping("/api/messages/{userId}")
    @ResponseBody
    public List<Message> getMessages(@PathVariable Long userId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            throw new RuntimeException("User not logged in");
        }
        
        // Mark messages as read when opening chat
        messageService.markAsRead(userId, currentUser.getId());
        
        return messageService.getChatMessages(currentUser.getId(), userId);
    }

    @PostMapping("/api/messages/media")
    @ResponseBody
    public Message uploadMedia(@RequestParam Long receiverId,
                              @RequestParam String messageType,
                              @RequestParam MultipartFile file,
                              HttpSession session) throws IOException {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            throw new RuntimeException("Not logged in");
        }

        String uploadDirStr = "uploads/chat_media/";
        File uploadDir = new File(uploadDirStr);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path path = Paths.get(uploadDirStr + fileName);
        Files.write(path, file.getBytes());
        String mediaUrl = "/uploads/chat_media/" + fileName;

        return messageService.saveMediaMessage(currentUser.getId(), receiverId, null, messageType, mediaUrl, null);
    }

    
    @GetMapping("/api/users")
    @ResponseBody
    public List<User> getAllUsers(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            throw new RuntimeException("User not logged in");
        }
        
        return userService.getAllUsers();
    }

    @GetMapping("/api/messages/search")
    @ResponseBody
    public List<Message> searchMessages(@RequestParam String query, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            throw new RuntimeException("User not logged in");
        }

        return messageService.searchMessages(currentUser.getId(), query);
    }

    @DeleteMapping("/api/messages/{messageId}")
    @ResponseBody
    public Message deleteMessage(@PathVariable Long messageId,
                                 @RequestParam(defaultValue = "false") boolean deleteForEveryone,
                                 HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            throw new RuntimeException("User not logged in");
        }

        return messageService.deleteMessage(messageId, currentUser.getId(), deleteForEveryone);
    }
    @GetMapping("/profile")
    public String showProfilePage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        // Refresh user data
        currentUser = userService.getUserById(currentUser.getId());
        model.addAttribute("currentUser", currentUser);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String about,
                                HttpSession session,
                                Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }

        User updatedUser = userService.updateProfile(currentUser.getId(), name, about, null);
        session.setAttribute("user", updatedUser);
        model.addAttribute("currentUser", updatedUser);
        model.addAttribute("success", "Profile updated successfully!");
        return "profile";
    }

    @GetMapping("/settings")
    public String showSettingsPage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentUser", currentUser);
        return "settings";
    }
}
