package com.lendshelf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.lendshelf.domain.Book;
import com.lendshelf.domain.Member;
import com.lendshelf.service.CatalogService;
import com.lendshelf.service.MemberService;
import com.lendshelf.service.ReservationService;
import org.springframework.mock.web.MockHttpSession;
import com.lendshelf.repository.LoanRepository;
import com.lendshelf.domain.Loan;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebPagesTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogService catalog;

    @Autowired
    MemberService members;

    @Autowired
    ReservationService reservations;

    @Autowired
    LoanRepository loans;

    @Test
    void catalogueRenders() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("A Brief History of Time")));
    }

    @Test
    void dashboardListsOverdueLoans() throws Exception {
        mvc.perform(get("/librarian"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Purple Hibiscus")))
                .andExpect(content().string(Matchers.containsString("Dune")));
    }

    @Test
    void registrationRequiresAnEmail() throws Exception {
        mvc.perform(post("/members").param("name", "Ngozi Obi").param("email", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Please enter an email address")));
    }

    @Test
    void registrationCreatesACard() throws Exception {
        mvc.perform(post("/members").param("name", "Ngozi Obi").param("email", "ngozi@example.com").param("phone", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/members/*"));
    }

    @Test
    void unknownBookIsNotFound() throws Exception {
        mvc.perform(get("/books/9999")).andExpect(status().isNotFound());
    }

    @Test
    void memberPageShowsTheUpdatedPlaceAfterSomeoneAheadCancels() throws Exception {
        Book book = catalog.search("Half of a Yellow Sun", 0).getContent().getFirst();
        var friend = reservations.reserve(members.register("Friend", "friend@example.com", null).getId(), book.getId());
        Member me = members.register("Me", "me@example.com", null);
        reservations.reserve(me.getId(), book.getId());

        mvc.perform(get("/members/" + me.getId()))
                .andExpect(content().string(Matchers.containsString("Number 4 in the queue")));

        reservations.cancel(friend.getId());

        mvc.perform(get("/members/" + me.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Number 3 in the queue")));
    }

    @Test
    void deskSessionsStaySeparate() throws Exception {
        List<Member> allMembers = members.all();
        Member adaeze = allMembers.get(0);
        Member tunde = allMembers.get(1);

        Book book = catalog.search("Things Fall Apart", 0).getContent().get(0);
        String barcode = catalog.copiesOf(book.getId()).get(0).getBarcode();

        MockHttpSession deskA = new MockHttpSession();
        MockHttpSession deskB = new MockHttpSession();

        mvc.perform(post("/desk/select-member").session(deskA).param("cardNumber", adaeze.getCardNumber()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/desk/select-member").session(deskB).param("cardNumber", tunde.getCardNumber()))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/desk/checkout").session(deskA).param("barcode", barcode))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/desk").session(deskA))
                .andExpect(content().string(Matchers.containsString("Serving " + adaeze.getName())));

        assertTrue(loans.findAll().stream()
                .anyMatch(loan -> loan.getMember().getId().equals(adaeze.getId())));
    }
}
