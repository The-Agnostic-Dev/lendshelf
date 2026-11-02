package com.lendshelf.domain;

import java.time.LocalDate;
import java.time.Period;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Copy copy;

    @ManyToOne(optional = false)
    private Member member;

    private LocalDate borrowedOn;
    private LocalDate dueOn;
    private LocalDate returnedOn;
    private int renewals;
    private double feeCharged;

    protected Loan() {
    }

    public Loan(Copy copy, Member member, LocalDate borrowedOn, LocalDate dueOn) {
        this.copy = copy;
        this.member = member;
        this.borrowedOn = borrowedOn;
        this.dueOn = dueOn;
    }

    public boolean isOpen() {
        return returnedOn == null;
    }

    public boolean isOverdue(LocalDate today) {
        return isOpen() && today.isAfter(dueOn);
    }

    /**
     * Whole days past the due date, or zero when the loan is not late.
     */
    public long daysOverdue(LocalDate today) {
        LocalDate end = isOpen() ? today : returnedOn;
        if (!end.isAfter(dueOn)) {
            return 0;
        }
        return Period.between(dueOn, end).getDays();
    }

    public void renew(LocalDate newDueOn) {
        this.dueOn = newDueOn;
        this.renewals++;
    }

    public void markReturned(LocalDate on, double fee) {
        this.returnedOn = on;
        this.feeCharged = fee;
    }

    public Long getId() {
        return id;
    }

    public Copy getCopy() {
        return copy;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getBorrowedOn() {
        return borrowedOn;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    public int getRenewals() {
        return renewals;
    }

    public double getFeeCharged() {
        return feeCharged;
    }
}
