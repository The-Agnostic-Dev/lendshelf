package com.lendshelf.web;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.lendshelf.domain.Loan;
import com.lendshelf.service.CatalogService;
import com.lendshelf.service.FeeCalculator;
import com.lendshelf.service.LoanService;
import com.lendshelf.service.ReservationService;

@Controller
public class DashboardController {

    public record OverdueRow(Loan loan, long daysOverdue, double fee) {
    }

    private final LoanService loans;
    private final CatalogService catalog;
    private final ReservationService reservations;
    private final FeeCalculator fees;
    private final Clock clock;

    public DashboardController(LoanService loans, CatalogService catalog, ReservationService reservations,
            FeeCalculator fees, Clock clock) {
        this.loans = loans;
        this.catalog = catalog;
        this.reservations = reservations;
        this.fees = fees;
        this.clock = clock;
    }

    @GetMapping("/librarian")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now(clock);
        List<OverdueRow> overdue = loans.overdueLoans().stream()
                .map(loan -> new OverdueRow(loan, loan.daysOverdue(today), fees.feeFor(loan)))
                .toList();

        model.addAttribute("overdue", overdue);
        model.addAttribute("titleCount", catalog.totalTitles());
        model.addAttribute("onLoan", loans.copiesOnLoan());
        model.addAttribute("waiting", reservations.waitingCount());
        model.addAttribute("today", today);
        return "dashboard";
    }
}
