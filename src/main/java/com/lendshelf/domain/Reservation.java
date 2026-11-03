package com.lendshelf.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Book book;

    @ManyToOne(optional = false)
    private Member member;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.WAITING;

    protected Reservation() {
    }

    public Reservation(Book book, Member member, LocalDateTime createdAt) {
        this.book = book;
        this.member = member;
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return status == ReservationStatus.WAITING || status == ReservationStatus.READY;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public Member getMember() {
        return member;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
