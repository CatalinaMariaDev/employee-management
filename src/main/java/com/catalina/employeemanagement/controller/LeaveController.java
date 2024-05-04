package com.catalina.employeemanagement.controller;

import com.catalina.employeemanagement.entity.CerereConcediu;
import com.catalina.employeemanagement.entity.StatusCerere;
import com.catalina.employeemanagement.entity.TipConcediu;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.UserRepository;
import com.catalina.employeemanagement.service.CerereConcediuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

@Controller
public class LeaveController {

    @Value("${leave.paid.max-days}")
    private int maxPaidLeaveDays;

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
            RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        // Validate dates
        if (dataInceput.after(dataSfarsit)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Data de început nu poate fi mai mare decât data de sfârșit!");
            return "redirect:/request_leave";
        }

        // Validate overlap
        List<CerereConcediu> overlappingRequests = service.findOverlappingRequests(user, dataInceput, dataSfarsit);
        if (!overlappingRequests.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Există deja o cerere de concediu în acea perioadă!");
            return "redirect:/request_leave";
        }

        // Validate paid leave duration
        if (tipConcediu == TipConcediu.CONCEDIU_PLATIT) {
            int daysTaken = service.calculateTotalPaidLeaveDays(user);
            int requestedDays = (int) ((dataSfarsit.getTime() - dataInceput.getTime()) / (1000 * 60 * 60 * 24)) + 1;
            if (daysTaken + requestedDays > maxPaidLeaveDays) {
                redirectAttributes.addFlashAttribute("errorMessage", "Numărul maxim de zile de concediu plătit este " + maxPaidLeaveDays + "!");
                return "redirect:/request_leave";
            }
        }

        // Validate medical leave attachment
        if (tipConcediu == TipConcediu.CONCEDIU_MEDICAL && file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Trebuie să atașați un fișier pentru concediu medical!");
            return "redirect:/request_leave";
        }

        CerereConcediu cerere = new CerereConcediu();
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
            redirectAttributes.addFlashAttribute("errorMessage", "Eroare la încărcarea fișierului!");
            return "redirect:/request_leave";
        }

        service.submitLeaveRequest(cerere);
        redirectAttributes.addFlashAttribute("success", true);
        return "redirect:/request_leave";
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

    @GetMapping("/generate_report")
    public ResponseEntity<byte[]> generateReport(@RequestParam("username") String username) {
        // Fetch the user's approved leaves
        List<CerereConcediu> approvedLeaves = service.findApprovedLeavesByUsername(username);
        byte[] reportData = new byte[0];

        try {
            // Convert the list to JSON
            ObjectMapper objectMapper = new ObjectMapper();
            reportData = objectMapper.writeValueAsBytes(approvedLeaves);
        } catch (Exception e) {
            // Handle exception appropriately
            e.printStackTrace();
        }

        // Prepare response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leaves_report.json");

        return ResponseEntity.ok()
                .headers(headers)
                .body(reportData);
    }

}
