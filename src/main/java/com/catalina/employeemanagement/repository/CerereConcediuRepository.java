package com.catalina.employeemanagement.repository;

import com.catalina.employeemanagement.entity.CerereConcediu;
import com.catalina.employeemanagement.entity.StatusCerere;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CerereConcediuRepository extends JpaRepository<CerereConcediu, Long> {
    int countByStatus(StatusCerere status);
}
