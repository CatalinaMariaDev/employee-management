package com.catalina.employeemanagement.service;

import com.catalina.employeemanagement.entity.CerereConcediu;
import com.catalina.employeemanagement.entity.StatusCerere;
import com.catalina.employeemanagement.entity.TipConcediu;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.CerereConcediuRepository;
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
public class CerereConcediuService {

    @Autowired
    private CerereConcediuRepository repository;

    @Value("${leave.paid.max-days}")
    private int maxPaidLeaveDays;

    @Transactional
    public CerereConcediu submitLeaveRequest(CerereConcediu cerere) {
      // AICI SE SALVEAZA CEREREA DE CONCEDIU IN BAZA DE DATE
        return repository.save(cerere);
    }

    public int countPendingRequests() {
        //AICI SE NUMARA REQUESTURILE IN ASTEPTARE PENTRU A NOTIFICA MANAGERUL IN MENIUL DIN DREAPTA A VIEW-ului
        return repository.countByStatus(StatusCerere.IN_ASTEPTARE);
    }

    public List<CerereConcediu> findPendingRequests() {
        return repository.findByStatus(StatusCerere.IN_ASTEPTARE);
    }

    public Optional<CerereConcediu> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public CerereConcediu save(CerereConcediu cerere) {
        return repository.save(cerere);
    }

    public List<CerereConcediu> findByUserName(String username) {
        return repository.findByUserUsername(username);
    }

    public List<CerereConcediu> findApprovedLeavesByUsername(String username) {
        List<CerereConcediu> userLeaves = repository.findByUserUsername(username);
        return userLeaves.stream()
                .filter(cerere -> cerere != null && cerere.getStatus() == StatusCerere.APROBAT)
                .collect(Collectors.toList());
    }

    public void delete(CerereConcediu cerereConcediu) {
        repository.delete(cerereConcediu);
    }

    public List<CerereConcediu> findOverlappingRequests(User user, Date startDate, Date endDate) {
        //AICI SE VALIDEAZA CERERILE DE CONCEDIU CARE SE POT SUPRAPUNE
        return repository.findByUserUsername(user.getUsername())
                .stream()
                .filter(cerere -> cerere.getStatus() != StatusCerere.RESPINS &&
                        (startDate.before(cerere.getDataSfarsit()) && endDate.after(cerere.getDataInceput())))
                .collect(Collectors.toList());
    }

    public int calculateTotalPaidLeaveDays(User user) {
        //AICI SE VALIDEAZA NUMARUL TOTAL DE ZILE PLATITE DE CONCEDIU CARE ESTE DEVINIT CA CONSTANTA IN PROPRIETATI
        return repository.findByUserUsername(user.getUsername())
                .stream()
                .filter(cerere -> cerere.getTipConcediu() == TipConcediu.CONCEDIU_PLATIT &&
                        cerere.getStatus() == StatusCerere.APROBAT)
                .mapToInt(cerere -> (int) ((cerere.getDataSfarsit().getTime() - cerere.getDataInceput().getTime()) / (1000 * 60 * 60 * 24)) + 1)
                .sum();
    }

    public boolean validateLeaveRequest(User user, TipConcediu tipConcediu, Date dataInceput, Date dataSfarsit, MultipartFile file) {
        //AICI AU LOC TOATE VALIDATILE CARE DUPA SA FIE AFISATE CA MESAJ USER-lui
        if (dataInceput.after(dataSfarsit)) {
            throw new IllegalArgumentException("Data de început nu poate fi mai mare decât data de sfârșit!");
        }

        if (!findOverlappingRequests(user, dataInceput, dataSfarsit).isEmpty()) {
            throw new IllegalArgumentException("Există deja o cerere de concediu în acea perioadă!");
        }

        if (tipConcediu == TipConcediu.CONCEDIU_PLATIT) {
            int daysTaken = calculateTotalPaidLeaveDays(user);
            int requestedDays = (int) ((dataSfarsit.getTime() - dataInceput.getTime()) / (1000 * 60 * 60 * 24)) + 1;
            if (daysTaken + requestedDays > maxPaidLeaveDays) {
                throw new IllegalArgumentException("Numărul maxim de zile de concediu plătit este " + maxPaidLeaveDays + "!");
            }
        }

        if (tipConcediu == TipConcediu.CONCEDIU_MEDICAL && file.isEmpty()) {
            throw new IllegalArgumentException("Trebuie să atașați un fișier pentru concediu medical!");
        }

        return true;
    }




}
