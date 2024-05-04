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
import java.util.stream.Collectors;

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
            model.addAttribute("error", "Eroare la încărcarea fișierului!");
            return "request_leave_page";
        }

        CerereConcediu savedCerere = service.submitLeaveRequest(cerere);

        model.addAttribute("success", true); // Pass success flag
        return "request_leave_page";
    }

    @GetMapping("/approve_leaves")
    public String showPendingRequests(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);

        // Alte variabile adăugate în model
        List<CerereConcediu> pendingRequests = service.findPendingRequests();
        model.addAttribute("pendingRequests", pendingRequests);

        return "aprobare_concedii_page";
    }


    @PostMapping("/update_status")
    public String updateLeaveRequestStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") StatusCerere status) {
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
        CerereConcediu cerere = service.findById(id).get();
        return ResponseEntity.ok(cerere.getComentarii());
    }

    @GetMapping("/get_file/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable Long id) {
        CerereConcediu cerere = service.findById(id).get();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // Sau schimbați formatul după cum este necesar
                .body(cerere.getFisierAtasat());
    }

    @GetMapping("/cereri_concediu")
    public String afiseazaCereriConcediu(Model model) {
        // Obțineți utilizatorul curent din contextul de securitate
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);

        // Căutați toate cererile de concediu ale utilizatorului curent
        List<CerereConcediu> cereriConcediu = service.findByUserName(username);

        // Adăugați lista de cereri de concediu la model
        model.addAttribute("cereriConcediu", cereriConcediu);

        return "cereri_concediu";
    }

    @GetMapping("/verifica_concedii")
    public String showLeaveVerificationPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        model.addAttribute("usernameforsearch", "");
        model.addAttribute("approvedLeaves", new ArrayList<>());
        return "verifica_concedii";
    }

    @PostMapping("/search_leaves")
    public String searchLeaves(@RequestParam("username") String username, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String usernamefornavbar = authentication.getName();
        model.addAttribute("username", usernamefornavbar);
        List<CerereConcediu> approvedLeaves = service.findApprovedLeavesByUsername(username);
        model.addAttribute("usernameforsearch", username);
        model.addAttribute("approvedLeaves", approvedLeaves);
        return "verifica_concedii";
    }

}
