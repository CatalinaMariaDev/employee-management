package com.catalina.employeemanagement.service;

import com.catalina.employeemanagement.entity.Role;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void postConstruct() {
        if (userRepository.findAll().isEmpty()) {
            User user = new User();
            user.setRole(Role.MANAGER);
            user.setUsername("admin");
            user.setEmail("admin@gmail.com");
            user.setPassword(passwordEncoder.encode("admin"));
            userRepository.save(user);
        }
    }

    public void register(User user) throws IllegalArgumentException {
        // Check for missing fields
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required!");
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email is required!");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required!");
        }

        // Check for duplicate email or username
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("The username " + user.getUsername() + " is already associated with an account!");
        }
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException("The email " + user.getEmail() + " is already associated with an account!");
        }

        // Set role
        if (userRepository.findAll().size() < 1) {
            user.setRole(Role.MANAGER);
        } else {
            user.setRole(Role.EMPLOYEE);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}