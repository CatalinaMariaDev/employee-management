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

    public void delete(LeaveRequest leaveRequest) {
        repository.delete(leaveRequest);
    }

    public List<LeaveRequest> findOverlappingRequests(User user, Date startDate, Date endDate) {
        //AICI SE VALIDEAZA CERERILE DE CONCEDIU CARE SE POT SUPRAPUNE
        return repository.findByUserUsername(user.getUsername())
                .stream()
                .filter(cerere -> cerere.getStatus() != RequestStatus.REJECTED &&
                        (startDate.before(cerere.getDataSfarsit()) && endDate.after(cerere.getDataInceput())))
                .collect(Collectors.toList());
    }

    public int calculateTotalPaidLeaveDays(User user) {
        //AICI SE VALIDEAZA NUMARUL TOTAL DE ZILE PLATITE DE CONCEDIU CARE ESTE DEVINIT CA CONSTANTA IN PROPRIETATI
        return repository.findByUserUsername(user.getUsername())
                .stream()
                .filter(cerere -> cerere.getTipConcediu() == RequestType.PAID_DAYS &&
                        cerere.getStatus() == RequestStatus.APPROVED)
                .mapToInt(cerere -> (int) ((cerere.getDataSfarsit().getTime() - cerere.getDataInceput().getTime()) / (1000 * 60 * 60 * 24)) + 1)
                .sum();
    }

    public boolean validateLeaveRequest(User user, RequestType tipConcediu, Date dataInceput, Date dataSfarsit, MultipartFile file) {
        //AICI AU LOC TOATE VALIDATILE CARE DUPA SA FIE AFISATE CA MESAJ USER-lui
        if (dataInceput.after(dataSfarsit)) {
            throw new IllegalArgumentException("Data de început nu poate fi mai mare decât data de sfârșit!");
        }

        if (!findOverlappingRequests(user, dataInceput, dataSfarsit).isEmpty()) {
            throw new IllegalArgumentException("Există deja o cerere de concediu în acea perioadă!");
        }

        if (tipConcediu == RequestType.PAID_DAYS) {
            int daysTaken = calculateTotalPaidLeaveDays(user);
            int requestedDays = (int) ((dataSfarsit.getTime() - dataInceput.getTime()) / (1000 * 60 * 60 * 24)) + 1;
            if (daysTaken + requestedDays > maxPaidLeaveDays) {
                throw new IllegalArgumentException("Numărul maxim de zile de concediu plătit este " + maxPaidLeaveDays + "!");
            }
        }

        if (tipConcediu == RequestType.MEDICAL_LEAVE && file.isEmpty()) {
            throw new IllegalArgumentException("Trebuie să atașați un fișier pentru concediu medical!");
        }

        return true;
    }




}
