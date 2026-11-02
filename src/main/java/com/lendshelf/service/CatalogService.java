package com.lendshelf.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lendshelf.config.LibraryProperties;
import com.lendshelf.domain.Book;
import com.lendshelf.domain.Copy;
import com.lendshelf.domain.CopyStatus;
import com.lendshelf.repository.BookRepository;
import com.lendshelf.repository.CopyRepository;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    public static final String AVAILABILITY_CACHE = "availability";

    private final BookRepository books;
    private final CopyRepository copies;
    private final LibraryProperties properties;

    public CatalogService(BookRepository books, CopyRepository copies, LibraryProperties properties) {
        this.books = books;
        this.copies = copies;
        this.properties = properties;
    }

    /**
     * Searches the catalogue by title or author.
     *
     * @param query free text, may be blank
     * @param page  zero-based page index
     */
    public Page<Book> search(String query, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), properties.pageSize(), Sort.by("title"));
        if (query == null || query.isBlank()) {
            return books.findAll(pageable);
        }
        String term = query.trim();
        return books.findByTitleContainingOrAuthorContaining(term, term, pageable);
    }

    public Book getBook(Long id) {
        return books.findById(id).orElseThrow(() -> new NotFoundException("Book", id));
    }

    public List<Copy> copiesOf(Long bookId) {
        return copies.findByBookIdOrderByBarcode(bookId);
    }

    /**
     * Copies currently on the shelf. Cached because the catalogue page asks for every
     * title on the page.
     */
    @Cacheable(AVAILABILITY_CACHE)
    public long availableCopies(Long bookId) {
        return copies.countByBookIdAndStatus(bookId, CopyStatus.AVAILABLE);
    }

    public long totalTitles() {
        return books.count();
    }
}
