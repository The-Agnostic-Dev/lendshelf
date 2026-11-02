package com.lendshelf.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lendshelf.domain.Loan;
import com.lendshelf.domain.Member;
import com.lendshelf.service.LibraryException;
import com.lendshelf.service.LoanService;
import com.lendshelf.service.MemberService;

@Controller
public class DeskController {

    private final DeskSession desk;
    private final MemberService members;
    private final LoanService loans;

    public DeskController(DeskSession desk, MemberService members, LoanService loans) {
        this.desk = desk;
        this.members = members;
        this.loans = loans;
    }

    @GetMapping("/desk")
    public String desk(Model model) {
        if (desk.isActive()) {
            model.addAttribute("member", members.get(desk.getMemberId()));
            model.addAttribute("loans", loans.currentLoans(desk.getMemberId()));
        }
        return "desk";
    }

    @PostMapping("/desk/member")
    public String serve(@RequestParam String cardNumber, RedirectAttributes flash) {
        try {
            Member member = members.byCard(cardNumber);
            desk.start(member.getId());
        } catch (LibraryException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/desk";
    }

    @PostMapping("/desk/checkout")
    public String checkout(@RequestParam String barcode, RedirectAttributes flash) {
        if (!desk.isActive()) {
            flash.addFlashAttribute("error", "Scan a library card first.");
            return "redirect:/desk";
        }
        try {
            Loan loan = loans.checkout(desk.getMemberId(), barcode);
            flash.addFlashAttribute("notice", "%s checked out to %s, due %s.".formatted(
                    loan.getCopy().getBook().getTitle(), loan.getMember().getName(), Dates.display(loan.getDueOn())));
        } catch (LibraryException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/desk";
    }

    @PostMapping("/desk/finish")
    public String finish(RedirectAttributes flash) {
        desk.finish();
        flash.addFlashAttribute("notice", "Desk is ready for the next member.");
        return "redirect:/desk";
    }
}
