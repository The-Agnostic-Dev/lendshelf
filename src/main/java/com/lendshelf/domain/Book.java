package com.lendshelf.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private String isbn;
    private int publishedYear;
    private String genre;

    @OneToMany(mappedBy = "book")
    @OrderBy("barcode")
    private List<Copy> copies = new ArrayList<>();

    protected Book() {
    }

    public Book(String title, String author, String isbn, int publishedYear, String genre) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
        this.genre = genre;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getPublishedYear() {
        return publishedYear;
    }

    public String getGenre() {
        return genre;
    }

    public List<Copy> getCopies() {
        return copies;
    }
}
