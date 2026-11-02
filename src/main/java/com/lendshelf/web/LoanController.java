package com.lendshelf.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lendshelf.domain.Loan;
import com.lendshelf.domain.Reservation;
import com.lendshelf.service.LibraryException;
import com.lendshelf.service.LoanService;
import com.lendshelf.service.ReservationService;

@Controller
public class LoanController {

    private final LoanService loans;
    private final ReservationService reservations;

    public LoanController(LoanService loans, ReservationService reservations) {
        this.loans = loans;
        this.reservations = reservations;
    }

    @PostMapping("/books/{bookId}/borrow")
    public String borrow(@PathVariable Long bookId, @RequestParam Long memberId, RedirectAttributes flash) {
        try {
            Loan loan = loans.borrow(memberId, bookId);
            flash.addFlashAttribute("notice", "%s is checked out until %s.".formatted(
                    loan.getCopy().getBook().getTitle(), Dates.display(loan.getDueOn())));
            return "redirect:/members/" + memberId;
        } catch (LibraryException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/books/" + bookId;
        }
    }

    @PostMapping("/books/{bookId}/reserve")
    public String reserve(@PathVariable Long bookId, @RequestParam Long memberId, RedirectAttributes flash) {
        try {
            Reservation reservation = reservations.reserve(memberId, bookId);
            flash.addFlashAttribute("notice", "Reserved " + reservation.getBook().getTitle() + ".");
            return "redirect:/members/" + memberId;
        } catch (LibraryException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/books/" + bookId;
        }
    }

    @PostMapping("/loans/{id}/return")
    public String returnLoan(@PathVariable Long id, @RequestParam Long memberId, RedirectAttributes flash) {
        try {
            Loan loan = loans.returnLoan(id);
            flash.addFlashAttribute("notice", "Returned " + loan.getCopy().getBook().getTitle() + ".");
        } catch (LibraryException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/members/" + memberId;
    }

    @PostMapping("/loans/{id}/renew")
    public String renew(@PathVariable Long id, @RequestParam Long memberId, RedirectAttributes flash) {
        try {
            Loan loan = loans.renew(id);
            flash.addFlashAttribute("notice", "Renewed %s, now due %s.".formatted(
                    loan.getCopy().getBook().getTitle(), Dates.display(loan.getDueOn())));
        } catch (LibraryException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/members/" + memberId;
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam Long memberId, RedirectAttributes flash) {
        try {
            Reservation reservation = reservations.cancel(id);
            flash.addFlashAttribute("notice", "Cancelled your reservation for " + reservation.getBook().getTitle() + ".");
        } catch (LibraryException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/members/" + memberId;
    }
}
