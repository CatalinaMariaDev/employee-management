package com.catalina.employeemanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "requests_leave")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_date", nullable = false)
    private Date startDate;

    @Column(name = "end_date", nullable = false)
    private Date endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private RequestType requestType;

    @Column(name = "comments")
    private String comments;

    @Lob
    @Column(name = "attached_file")
    private byte[] fisierAtasat;

}
