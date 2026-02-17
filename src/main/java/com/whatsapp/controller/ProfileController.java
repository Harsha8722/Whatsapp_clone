package com.whatsapp.controller;

import com.whatsapp.entity.User;
import com.whatsapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    private static final String UPLOAD_DIR = "uploads/profile_photos/";

    @PostMapping("/update")
    @ResponseBody
    public User updateProfile(@RequestParam(required = false) String name,
                             @RequestParam(required = false) String about,
                             @RequestParam(required = false) MultipartFile photo,
                             HttpSession session) throws IOException {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            throw new RuntimeException("Not logged in");
        }

        String photoPath = null;
        if (photo != null && !photo.isEmpty()) {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String fileName = UUID.randomUUID().toString() + "_" + photo.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.write(path, photo.getBytes());
            photoPath = "/uploads/profile_photos/" + fileName;
        }

        User updatedUser = userService.updateProfile(currentUser.getId(), name, about, photoPath);
        session.setAttribute("user", updatedUser);
        return updatedUser;
    }
}
