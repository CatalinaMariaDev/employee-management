package com.catalina.employeemanagement.controller;

import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.service.UserService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String getLoginPage() {
        return "login_page";
    }

    @GetMapping("/registration")
    public String getRegistrationPage(Model model) {
        model.addAttribute("user", new User());
        return "registration_page";
    }

    @PostMapping("/registration")
    public String registerUser(@ModelAttribute User user) {
        userService.register(user);
        return "redirect:/login?success";
    }

    @GetMapping("/view_users")
    public String showUsersPage(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        return "view_users";
    }
}
