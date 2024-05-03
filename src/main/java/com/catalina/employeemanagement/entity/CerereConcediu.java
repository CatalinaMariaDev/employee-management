package com.catalina.employeemanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "cereri_concediu")
@Getter
@Setter
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

    @Enumerated(EnumType.STRING)
    @Column(name = "tip_concediu", nullable = false)
    private TipConcediu tipConcediu;

    @Column(name = "comentarii")
    private String comentarii;

    @Lob
    @Column(name = "fisier_atașat")
    private byte[] fisierAtasat;

}
