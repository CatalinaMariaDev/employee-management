package com.catalina.employeemanagement.entity;

import javax.persistence.*;
import java.util.Date;

import lombok.*;

@Entity
@Table(name = "cereri_concediu")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CerereConcediu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "data_inceput", nullable = false)
    private Date dataInceput;

    @Column(name = "data_sfarsit", nullable = false)
    private Date dataSfarsit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCerere status;

    // Alte atribute și metode
}

