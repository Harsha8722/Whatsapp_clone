package com.whatsapp.repository;

import com.whatsapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByPhone(String phone);
    
    Optional<User> findByPhoneAndPassword(String phone, String password);
    
    List<User> findAllByOrderByNameAsc();
    
    boolean existsByPhone(String phone);
}
