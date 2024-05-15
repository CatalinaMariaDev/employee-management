package com.catalina.employeemanagement.service;

import com.catalina.employeemanagement.entity.Role;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.UserRepository;
import lombok.AllArgsConstructor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class UserService {


    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void postConstruct() {
        User user = new User();
        user.setRole(Role.MANAGER);
        user.setUsername("admin");
        user.setPassword("abc");
        //userRepository.save(user);
    }

    public void register(User user) {
        if (userRepository.findAll().size() < 1) {
            user.setRole(Role.MANAGER);
        } else {
            user.setRole(Role.LUCRATOR);
        }
        user.setPassword(user.getPassword());
        userRepository.save(user);
    }
}