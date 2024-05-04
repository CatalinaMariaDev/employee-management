package com.catalina.employeemanagement.service;

import com.catalina.employeemanagement.entity.CerereConcediu;
import com.catalina.employeemanagement.entity.StatusCerere;
import com.catalina.employeemanagement.entity.User;
import com.catalina.employeemanagement.repository.CerereConcediuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
