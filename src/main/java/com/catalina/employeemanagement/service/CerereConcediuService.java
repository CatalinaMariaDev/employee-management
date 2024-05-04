package com.catalina.employeemanagement.service;

import com.catalina.employeemanagement.entity.CerereConcediu;
import com.catalina.employeemanagement.entity.StatusCerere;
import com.catalina.employeemanagement.entity.TipConcediu;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.CerereConcediuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CerereConcediuService {

    @Autowired
    private CerereConcediuRepository repository;

    @Transactional
    public CerereConcediu submitLeaveRequest(CerereConcediu cerere) {
        // Adaugă aici orice validări sau preprocesări necesare
        return repository.save(cerere);
    }

    public List<CerereConcediu> findOverlappingRequests(User user, Date startDate, Date endDate) {
        return repository.findByUserUsername(user.getUsername())
                .stream()
                .filter(cerere -> cerere.getStatus() != StatusCerere.RESPINS &&
                        (startDate.before(cerere.getDataSfarsit()) && endDate.after(cerere.getDataInceput())))
                .collect(Collectors.toList());
    }

    public int calculateTotalPaidLeaveDays(User user) {
        return repository.findByUserUsername(user.getUsername())
                .stream()
                .filter(cerere -> cerere.getTipConcediu() == TipConcediu.CONCEDIU_PLATIT &&
                        cerere.getStatus() == StatusCerere.APROBAT)
                .mapToInt(cerere -> (int) ((cerere.getDataSfarsit().getTime() - cerere.getDataInceput().getTime()) / (1000 * 60 * 60 * 24)) + 1)
                .sum();
    }

    public int countPendingRequests() {
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




}
