package com.catalina.employeemanagement.controller;

import com.catalina.employeemanagement.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AboutController {
    @Autowired
    LeaveRequestService service;
    @GetMapping("/about")
    public String showLeaveRequestForm(Model model) {
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        return "about_page";
    }

}
