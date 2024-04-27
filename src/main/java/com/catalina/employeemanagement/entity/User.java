package com.catalina.employeemanagement.entity;

import javax.persistence.*;

import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nume;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role rol;
}

