package com.catalina.employeemanagement.repository;

import com.catalina.employeemanagement.entity.LeaveRequest;
import com.catalina.employeemanagement.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    int countByStatus(RequestStatus status);

    List<LeaveRequest> findByStatus(RequestStatus status);

    List<LeaveRequest> findByUserUsername(String userUsername);
    List<LeaveRequest> findByUserEmail(String email);
}
