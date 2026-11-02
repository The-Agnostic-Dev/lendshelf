package com.lendshelf.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * A physical copy of a book on the shelf, identified by the barcode sticker on its spine.
 */
@Entity
public class Copy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Book book;

    @Column(unique = true, nullable = false)
    private String barcode;

    @Enumerated(EnumType.STRING)
    private CopyStatus status = CopyStatus.AVAILABLE;

    protected Copy() {
    }

    public Copy(Book book, String barcode) {
        this.book = book;
        this.barcode = barcode;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public String getBarcode() {
        return barcode;
    }

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status;
    }
}
