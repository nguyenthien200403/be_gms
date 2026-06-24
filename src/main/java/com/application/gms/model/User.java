package com.application.gms.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name ="Users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="username")
    private String userName;
    @Column(name ="password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private RoleEnum roleEnum;
}
