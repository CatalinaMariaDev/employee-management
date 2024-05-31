package com.catalina.employeemanagement.controller;

import com.catalina.employeemanagement.entity.LeaveRequest;
import com.catalina.employeemanagement.entity.RequestStatus;
import com.catalina.employeemanagement.entity.RequestType;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.UserRepository;
import com.catalina.employeemanagement.service.LeaveRequestService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;


/* AICI NOI DEFINIM ENDPOINTURILE PENTRU FUNCTIONALUL APLICATIEI NOASTRE
FIECARE ENDPOINT RETURNEAZA UN STRING ACESTA FIIND FILENAME-UL TEMPLATURILOR HTML PE CARE THYMELEAF LE CITESTE
 */
@Controller
public class LeaveController {

    @Autowired
    private LeaveRequestService service;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/request_leave")
    public String showLeaveRequestForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        model.addAttribute("username", username);
        model.addAttribute("types", RequestType.values());
        return "request_leave_page";
    }

    @PostMapping("/submit")
    public String submitLeaveRequest(
            @RequestParam("requestType") RequestType requestType,
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam("comments") String comments,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setUser(user);
        leaveRequest.setRequestType(requestType);
        leaveRequest.setStartDate(startDate);
        leaveRequest.setEndDate(endDate);
        leaveRequest.setComments(comments);
        leaveRequest.setStatus(RequestStatus.WAITING);

        try {
            service.validateLeaveRequest(user, requestType, startDate, endDate, file);
            if (!file.isEmpty()) {
                leaveRequest.setAttachedFile(file.getBytes());
            }
            service.submitLeaveRequest(leaveRequest);
            redirectAttributes.addFlashAttribute("success", true);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fail to upload file");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/request_leave";
    }


    @GetMapping("/approve_leaves")
    public String showPendingRequests(Model model) {
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        List<LeaveRequest> pendingRequests = service.findPendingRequests();
        model.addAttribute("pendingRequests", pendingRequests);
        return "aprove_request_leave_page";
    }

    @PostMapping("/update_status")
    public String updateLeaveRequestStatus(@RequestParam("id") Long id, @RequestParam("status") RequestStatus status) {

        Optional<LeaveRequest> cerereOptional = service.findById(id);
        if (cerereOptional.isPresent()) {
            LeaveRequest cerere = cerereOptional.get();
            cerere.setStatus(status);
            service.submitLeaveRequest(cerere);
        }
        return "redirect:/approve_leaves";
    }

    @GetMapping("/get_comments/{id}")
    public ResponseEntity<String> getComments(@PathVariable Long id) {

        LeaveRequest cerere = service.findById(id).orElse(null);
        if (cerere == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cerere.getComments());
    }

    @GetMapping("/get_file/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable Long id) {

        LeaveRequest cerere = service.findById(id).orElse(null);
        if (cerere == null || cerere.getAttachedFile() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(cerere.getAttachedFile());
    }

    @GetMapping("/requests_leave")
    public String displayRequestsLeave(Model model) {
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        List<LeaveRequest> leaveRequests = service.findByUserName(username);
        model.addAttribute("leaveRequests", leaveRequests);
        return "requests_leave";
    }

    @GetMapping("/check_requests")
    public String showLeaveVerificationPage(Model model) {
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        model.addAttribute("usernameforsearch", "");
        model.addAttribute("approvedLeaves", new ArrayList<>());
        return "check_requests";
    }

    @PostMapping("/search_leaves")
    public String searchLeaves(@RequestParam("email") String email, Model model) {
        int pendingRequestsCount = service.countPendingRequests();
        model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailfornavbar = authentication.getName();
        model.addAttribute("username", emailfornavbar);
        List<LeaveRequest> approvedLeaves = service.findApprovedLeavesByEmail(email);
        model.addAttribute("emailforsearch", email);
        model.addAttribute("approvedLeaves", approvedLeaves);
        return "check_requests";
    }

    @PostMapping("/cancel_leave")
    public String cancelLeaveRequest(@RequestParam("id") Long id) {
        Optional<LeaveRequest> cerereOptional = service.findById(id);
        if (cerereOptional.isPresent()) {
            LeaveRequest cerere = cerereOptional.get();
            if (cerere.getStatus() == RequestStatus.WAITING) {
                service.delete(cerere);
            }
        }
        return "redirect:/requests_leave";
    }

    @GetMapping("/generate_report")
    public ResponseEntity<byte[]> generateReport(@RequestParam("email") String email) {
        User user = userRepository.findByEmail(email);
        List<LeaveRequest> approvedLeaves = service.findApprovedLeavesByEmail(email);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(byteStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.setTextAlignment(TextAlignment.LEFT);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            document.add(new Paragraph("Leave Report for " + user.getUsername())
                    .setFont(PdfFontFactory.createFont(StandardFonts.TIMES_BOLD))
                    .setFontSize(14));

            for (LeaveRequest leave : approvedLeaves) {
                String period = sdf.format(leave.getStartDate()) + " - " + sdf.format(leave.getEndDate());
                document.add(new Paragraph(period + ": " + leave.getRequestType().name() +
                        (leave.getComments() != null ? " - Comments: " + leave.getComments() : "")));
            }
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leaves_report.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(byteStream.toByteArray());
    }

}
