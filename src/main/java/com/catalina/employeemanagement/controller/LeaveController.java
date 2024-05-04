package com.catalina.employeemanagement.controller;

import com.catalina.employeemanagement.entity.CerereConcediu;
import com.catalina.employeemanagement.entity.StatusCerere;
import com.catalina.employeemanagement.entity.TipConcediu;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.UserRepository;
import com.catalina.employeemanagement.service.CerereConcediuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Controller
public class LeaveController {

    @Autowired
    private CerereConcediuService service;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/request_leave")
    public String showLeaveRequestForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        model.addAttribute("username", username);
        model.addAttribute("types", TipConcediu.values());
        return "request_leave_page";
    }

    @PostMapping("/submit")
    public String submitLeaveRequest(
            @RequestParam("tipConcediu") TipConcediu tipConcediu,
            @RequestParam("dataInceput") @DateTimeFormat(pattern = "yyyy-MM-dd") Date dataInceput,
            @RequestParam("dataSfarsit") @DateTimeFormat(pattern = "yyyy-MM-dd") Date dataSfarsit,
            @RequestParam("comentarii") String comentarii,
            @RequestParam("file") MultipartFile file,
            Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        CerereConcediu cerere = new CerereConcediu();
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        cerere.setUser(user);
        cerere.setTipConcediu(tipConcediu);
        cerere.setDataInceput(dataInceput);
        cerere.setDataSfarsit(dataSfarsit);
        cerere.setComentarii(comentarii);
        cerere.setStatus(StatusCerere.IN_ASTEPTARE);

        try {
            if (!file.isEmpty()) {
                cerere.setFisierAtasat(file.getBytes());
            }
        } catch (IOException e) {
            model.addAttribute("error", "Eroare la încărcarea fișierului!");
            return "request_leave_page";
        }

        CerereConcediu savedCerere = service.submitLeaveRequest(cerere);
        model.addAttribute("success", true);
        return "request_leave_page";
    }

    @GetMapping("/approve_leaves")
    public String showPendingRequests(Model model) {
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        List<CerereConcediu> pendingRequests = service.findPendingRequests();
        model.addAttribute("pendingRequests", pendingRequests);
        return "aprobare_concedii_page";
    }

    @PostMapping("/update_status")
    public String updateLeaveRequestStatus(@RequestParam("id") Long id, @RequestParam("status") StatusCerere status) {

        Optional<CerereConcediu> cerereOptional = service.findById(id);
        if (cerereOptional.isPresent()) {
            CerereConcediu cerere = cerereOptional.get();
            cerere.setStatus(status);
            service.submitLeaveRequest(cerere);
        }
        return "redirect:/approve_leaves";
    }

    @GetMapping("/get_comments/{id}")
    public ResponseEntity<String> getComments(@PathVariable Long id) {

        CerereConcediu cerere = service.findById(id).orElse(null);
        if (cerere == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cerere.getComentarii());
    }

    @GetMapping("/get_file/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable Long id) {

        CerereConcediu cerere = service.findById(id).orElse(null);
        if (cerere == null || cerere.getFisierAtasat() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(cerere.getFisierAtasat());
    }

    @GetMapping("/cereri_concediu")
    public String afiseazaCereriConcediu(Model model) {
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        List<CerereConcediu> cereriConcediu = service.findByUserName(username);
        model.addAttribute("cereriConcediu", cereriConcediu);
        return "cereri_concediu";
    }

    @GetMapping("/verifica_concedii")
    public String showLeaveVerificationPage(Model model) {
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        model.addAttribute("usernameforsearch", "");
        model.addAttribute("approvedLeaves", new ArrayList<>());
        return "verifica_concedii";
    }

    @PostMapping("/search_leaves")
    public String searchLeaves(@RequestParam("username") String username, Model model) {
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String usernamefornavbar = authentication.getName();
        model.addAttribute("username", usernamefornavbar);
        List<CerereConcediu> approvedLeaves = service.findApprovedLeavesByUsername(username);
        model.addAttribute("usernameforsearch", username);
        model.addAttribute("approvedLeaves", approvedLeaves);
        return "verifica_concedii";
    }

    @PostMapping("/cancel_leave")
    public String cancelLeaveRequest(@RequestParam("id") Long id) {
        Optional<CerereConcediu> cerereOptional = service.findById(id);
        if (cerereOptional.isPresent()) {
            CerereConcediu cerere = cerereOptional.get();
            if (cerere.getStatus() == StatusCerere.IN_ASTEPTARE) {
                service.delete(cerere);
            }
        }
        return "redirect:/cereri_concediu";
    }

}
