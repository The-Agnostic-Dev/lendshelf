package com.lendshelf.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lendshelf.domain.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    Page<Book> findByTitleContainingOrAuthorContaining(String title, String author, Pageable pageable);
}
