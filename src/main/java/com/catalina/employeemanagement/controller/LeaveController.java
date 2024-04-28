package com.catalina.employeemanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LeaveController {

    @GetMapping("/request_leave")
    public String showLeaveRequestForm(Model model) {
        model.addAttribute("username", "Catalina Maria");
        return "request_leave_page";
    }
}
