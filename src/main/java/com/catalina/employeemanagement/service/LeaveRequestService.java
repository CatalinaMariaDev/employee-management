package com.catalina.employeemanagement.service;

import com.catalina.employeemanagement.entity.LeaveRequest;
import com.catalina.employeemanagement.entity.RequestStatus;
import com.catalina.employeemanagement.entity.RequestType;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/* AICI ESTE FUNCTIONALUL DELEGAT PENTRU CONTROLERUL APLICATIEI LeaveController
IN MARE PARTE AICI SE ASCUNDE TOATA LOGICA APLICATIEI
 */
@Service
public class LeaveRequestService {

    @Autowired
    private LeaveRequestRepository repository;

    @Value("${leave.paid.max-days}")
    private int maxPaidLeaveDays;

    @Transactional
    public LeaveRequest submitLeaveRequest(LeaveRequest cerere) {
      // AICI SE SALVEAZA CEREREA DE CONCEDIU IN BAZA DE DATE
        return repository.save(cerere);
    }

    public int countPendingRequests() {
        //AICI SE NUMARA REQUESTURILE IN ASTEPTARE PENTRU A NOTIFICA MANAGERUL IN MENIUL DIN DREAPTA A VIEW-ului
        return repository.countByStatus(RequestStatus.WAITING);
    }

    public List<LeaveRequest> findPendingRequests() {
        return repository.findByStatus(RequestStatus.WAITING);
    }

    public Optional<LeaveRequest> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public LeaveRequest save(LeaveRequest cerere) {
        return repository.save(cerere);
    }

    public List<LeaveRequest> findByUserName(String username) {
        return repository.findByUserUsername(username);
    }

    public List<LeaveRequest> findApprovedLeavesByUsername(String username) {
        List<LeaveRequest> userLeaves = repository.findByUserUsername(username);
        return userLeaves.stream()
                .filter(cerere -> cerere != null && cerere.getStatus() == RequestStatus.APPROVED)
                .collect(Collectors.toList());
    }

    public List<LeaveRequest> findApprovedLeavesByEmail(String email) {
        List<LeaveRequest> userLeaves = repository.findByUserEmail(email);
        return userLeaves.stream()
                .filter(cerere -> cerere != null && cerere.getStatus() == RequestStatus.APPROVED)
                .collect(Collectors.toList());
    }

    public void delete(LeaveRequest leaveRequest) {
        repository.delete(leaveRequest);
    }

    public List<LeaveRequest> findOverlappingRequests(User user, Date startDate, Date endDate) {
        //AICI SE VALIDEAZA CERERILE DE CONCEDIU CARE SE POT SUPRAPUNE
        return repository.findByUserUsername(user.getUsername())
                .stream()
                .filter(cerere -> cerere.getStatus() != RequestStatus.REJECTED &&
                        (startDate.before(cerere.getEndDate()) && endDate.after(cerere.getStartDate())))
                .collect(Collectors.toList());
    }

    public int calculateTotalPaidLeaveDays(User user) {
        //AICI SE VALIDEAZA NUMARUL TOTAL DE ZILE PLATITE DE CONCEDIU CARE ESTE DEVINIT CA CONSTANTA IN PROPRIETATI
        return repository.findByUserUsername(user.getUsername())
                .stream()
                .filter(cerere -> cerere.getRequestType() == RequestType.PAID_DAYS &&
                        cerere.getStatus() == RequestStatus.APPROVED)
                .mapToInt(cerere -> (int) ((cerere.getEndDate().getTime() - cerere.getStartDate().getTime()) / (1000 * 60 * 60 * 24)) + 1)
                .sum();
    }

    public boolean validateLeaveRequest(User user, RequestType leaveRequest, Date startDate, Date endDate, MultipartFile file) {
        //AICI AU LOC TOATE VALIDATILE CARE DUPA SA FIE AFISATE CA MESAJ USER-lui
        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("Start date can't be bigger that end date!");
        }

        if (!findOverlappingRequests(user, startDate, endDate).isEmpty()) {
            throw new IllegalArgumentException("Already exists a request leave for that period!");
        }

        if (leaveRequest == RequestType.PAID_DAYS) {
            int daysTaken = calculateTotalPaidLeaveDays(user);
            int requestedDays = (int) ((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)) + 1;
            if (daysTaken + requestedDays > maxPaidLeaveDays) {
                throw new IllegalArgumentException("The maximum number of paid days is:" + maxPaidLeaveDays + "!");
            }
        }

        if (leaveRequest == RequestType.MEDICAL_LEAVE && file.isEmpty()) {
            throw new IllegalArgumentException("You need to attach a file for medical leave!");
        }

        return true;
    }




}
