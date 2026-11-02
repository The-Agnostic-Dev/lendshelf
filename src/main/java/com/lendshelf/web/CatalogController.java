package com.lendshelf.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.lendshelf.domain.Book;
import com.lendshelf.service.CatalogService;
import com.lendshelf.service.MemberService;
import com.lendshelf.service.ReservationService;

@Controller
public class CatalogController {

    private final CatalogService catalog;
    private final MemberService members;
    private final ReservationService reservations;

    public CatalogController(CatalogService catalog, MemberService members, ReservationService reservations) {
        this.catalog = catalog;
        this.members = members;
        this.reservations = reservations;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/books";
    }

    /**
     * The public catalogue. Pages are numbered from 1 in the URL.
     */
    @GetMapping("/books")
    public String list(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        Page<Book> results = catalog.search(q, page - 1);
        Map<Long, Long> available = new LinkedHashMap<>();
        results.forEach(book -> available.put(book.getId(), catalog.availableCopies(book.getId())));

        model.addAttribute("page", results);
        model.addAttribute("available", available);
        model.addAttribute("q", q);
        return "books/list";
    }

    @GetMapping("/books/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Book book = catalog.getBook(id);
        model.addAttribute("book", book);
        model.addAttribute("copies", catalog.copiesOf(id));
        model.addAttribute("availableCount", catalog.availableCopies(id));
        model.addAttribute("queue", reservations.queueFor(id));
        model.addAttribute("members", members.all());
        return "books/detail";
    }
}
