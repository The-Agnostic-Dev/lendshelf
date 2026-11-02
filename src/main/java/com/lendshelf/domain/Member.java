package com.lendshelf.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;

    @Column(unique = true, nullable = false)
    private String cardNumber;

    private LocalDate joinedOn;

    protected Member() {
    }

    public Member(String name, String email, String phone, String cardNumber, LocalDate joinedOn) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.cardNumber = cardNumber;
        this.joinedOn = joinedOn;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public LocalDate getJoinedOn() {
        return joinedOn;
    }
}
