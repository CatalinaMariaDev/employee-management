package com.catalina.employeemanagement.controller;

import com.catalina.employeemanagement.entity.CerereConcediu;
import com.catalina.employeemanagement.entity.StatusCerere;
import com.catalina.employeemanagement.entity.TipConcediu;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.UserRepository;
import com.catalina.employeemanagement.service.CerereConcediuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;

@Controller
public class LeaveController {

    @Autowired
    private CerereConcediuService service;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/request_leave")
    public String showLeaveRequestForm(Model model) {
        model.addAttribute("types", TipConcediu.values());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);

        // Obține numărul de cereri în așteptare
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);

        return "request_leave_page";
    }

    @PostMapping("/submit")
    public ResponseEntity<CerereConcediu> submitLeaveRequest(
            @RequestParam("tipConcediu") TipConcediu tipConcediu,
            @RequestParam("dataInceput") @DateTimeFormat(pattern = "yyyy-MM-dd") Date dataInceput,
            @RequestParam("dataSfarsit") @DateTimeFormat(pattern = "yyyy-MM-dd") Date dataSfarsit,
            @RequestParam("comentarii") String comentarii,
            @RequestParam("file") MultipartFile file) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        CerereConcediu cerere = new CerereConcediu();
        cerere.setUser(user);
        cerere.setTipConcediu(tipConcediu);
        cerere.setDataInceput(dataInceput);
        cerere.setDataSfarsit(dataSfarsit);
        cerere.setComentarii(comentarii);

        // Inițializează statusul la IN_ASTEPTARE
        cerere.setStatus(StatusCerere.IN_ASTEPTARE);

        try {
            if (!file.isEmpty()) {
                cerere.setFisierAtasat(file.getBytes());
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        CerereConcediu savedCerere = service.submitLeaveRequest(cerere);
        return ResponseEntity.ok(savedCerere);
    }


}
