package com.catalina.employeemanagement.controller;

import com.catalina.employeemanagement.entity.CerereConcediu;
import com.catalina.employeemanagement.entity.StatusCerere;
import com.catalina.employeemanagement.entity.TipConcediu;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.UserRepository;
import com.catalina.employeemanagement.service.CerereConcediuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
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
            RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        CerereConcediu cerere = new CerereConcediu();
        cerere.setUser(user);
        cerere.setTipConcediu(tipConcediu);
        cerere.setDataInceput(dataInceput);
        cerere.setDataSfarsit(dataSfarsit);
        cerere.setComentarii(comentarii);
        cerere.setStatus(StatusCerere.IN_ASTEPTARE);

        try {
            service.validateLeaveRequest(user, tipConcediu, dataInceput, dataSfarsit, file);
            if (!file.isEmpty()) {
                cerere.setFisierAtasat(file.getBytes());
            }
            service.submitLeaveRequest(cerere);
            redirectAttributes.addFlashAttribute("success", true);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Eroare la încărcarea fișierului!");
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
        List<CerereConcediu> approvedLeaves = service.findApprovedLeavesByUsername(username);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(byteStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.setTextAlignment(TextAlignment.LEFT);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            document.add(new Paragraph("Leave Report for " + username)
                    .setFont(PdfFontFactory.createFont(StandardFonts.TIMES_BOLD))
                    .setFontSize(14));

            for (CerereConcediu leave : approvedLeaves) {
                String period = sdf.format(leave.getDataInceput()) + " - " + sdf.format(leave.getDataSfarsit());
                document.add(new Paragraph(period + ": " + leave.getTipConcediu().name() +
                        (leave.getComentarii() != null ? " - Comments: " + leave.getComentarii() : "")));
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
