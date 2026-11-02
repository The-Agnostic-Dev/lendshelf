package com.lendshelf.web;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lendshelf.config.LibraryProperties;
import com.lendshelf.domain.Loan;
import com.lendshelf.domain.Member;
import com.lendshelf.service.FeeCalculator;
import com.lendshelf.service.LoanService;
import com.lendshelf.service.MemberService;
import com.lendshelf.service.ReservationService;

import jakarta.validation.Valid;

@Controller
public class MemberController {

    private final MemberService members;
    private final LoanService loans;
    private final ReservationService reservations;
    private final FeeCalculator fees;
    private final LibraryProperties properties;
    private final Clock clock;

    public MemberController(MemberService members, LoanService loans, ReservationService reservations,
            FeeCalculator fees, LibraryProperties properties, Clock clock) {
        this.members = members;
        this.loans = loans;
        this.reservations = reservations;
        this.fees = fees;
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping("/members")
    public String list(Model model) {
        model.addAttribute("members", members.all());
        return "members/list";
    }

    @GetMapping("/members/new")
    public String newMember(Model model) {
        model.addAttribute("form", new MemberForm());
        return "members/new";
    }

    @PostMapping("/members")
    public String create(@Valid @ModelAttribute("form") MemberForm form, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) {
            return "members/new";
        }
        Member member = members.register(form.getName(), form.getEmail(), form.getPhone());
        flash.addFlashAttribute("notice", "Welcome, %s. Library card %s is ready.".formatted(member.getName(), member.getCardNumber()));
        return "redirect:/members/" + member.getId();
    }

    @GetMapping("/members/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Member member = members.get(id);
        List<Loan> current = loans.currentLoans(id);
        List<Loan> past = loans.pastLoans(id);

        Map<Long, Double> feeByLoan = new LinkedHashMap<>();
        double outstanding = 0;
        for (Loan loan : current) {
            double fee = fees.feeFor(loan);
            feeByLoan.put(loan.getId(), fee);
            outstanding += fee;
        }
        for (Loan loan : past) {
            feeByLoan.put(loan.getId(), loan.getFeeCharged());
            outstanding += loan.getFeeCharged();
        }

        model.addAttribute("member", member);
        model.addAttribute("currentLoans", current);
        model.addAttribute("pastLoans", past);
        model.addAttribute("reservations", reservations.activeFor(id));
        model.addAttribute("fees", feeByLoan);
        model.addAttribute("outstanding", outstanding);
        model.addAttribute("maxRenewals", properties.maxRenewals());
        model.addAttribute("today", LocalDate.now(clock));
        return "members/detail";
    }
}
