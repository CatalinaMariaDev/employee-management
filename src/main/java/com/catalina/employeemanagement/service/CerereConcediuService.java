package com.catalina.employeemanagement.service;

import com.catalina.employeemanagement.entity.CerereConcediu;
import com.catalina.employeemanagement.entity.StatusCerere;
import com.catalina.employeemanagement.repository.CerereConcediuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

}
