package com.whatsapp.service;

import com.whatsapp.entity.User;
import com.whatsapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User registerUser(String name, String phone, String password) {
        // Check if user already exists
        if (userRepository.existsByPhone(phone)) {
            throw new RuntimeException("User with this phone number already exists");
        }
        
        User user = new User(name, phone, password);
        return userRepository.save(user);
    }

    public User loginUser(String phone, String password) {
        Optional<User> userOpt = userRepository.findByPhoneAndPassword(phone, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus("online");
            return userRepository.save(user);
        }
        throw new RuntimeException("Invalid phone or password");
    }

    public void logoutUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus("offline");
            userRepository.save(user);
        }
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByPhone(String phone) {
        return userRepository.findByPhone(phone).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByNameAsc();
    }

    public void updateUserStatus(Long userId, String status) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(status);
            userRepository.save(user);
        }
    }

    public User updateProfile(Long userId, String name, String about, String profilePhoto) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (name != null) user.setName(name);
            if (about != null) user.setAbout(about);
            if (profilePhoto != null) user.setProfilePhoto(profilePhoto);
            return userRepository.save(user);
        }
        return null;
    }
}
