package com.lendshelf.seed;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.lendshelf.config.LibraryProperties;
import com.lendshelf.domain.Book;
import com.lendshelf.domain.Copy;
import com.lendshelf.domain.CopyStatus;
import com.lendshelf.domain.Loan;
import com.lendshelf.domain.Member;
import com.lendshelf.domain.Reservation;
import com.lendshelf.repository.BookRepository;
import com.lendshelf.repository.CopyRepository;
import com.lendshelf.repository.LoanRepository;
import com.lendshelf.repository.MemberRepository;
import com.lendshelf.repository.ReservationRepository;

/**
 * Loads a small branch catalogue so the app is usable straight after start-up.
 * Loan dates are relative to today. Runs before the web server starts taking requests.
 */
@Component
public class DemoDataLoader implements SmartInitializingSingleton {

    private final BookRepository books;
    private final CopyRepository copies;
    private final MemberRepository members;
    private final LoanRepository loans;
    private final ReservationRepository reservations;
    private final LibraryProperties properties;
    private final TransactionTemplate transactions;
    private final Clock clock;

    private final Map<String, List<Copy>> copiesByTitle = new HashMap<>();
    private final Map<String, Member> membersByName = new HashMap<>();

    public DemoDataLoader(BookRepository books, CopyRepository copies, MemberRepository members, LoanRepository loans,
            ReservationRepository reservations, LibraryProperties properties, TransactionTemplate transactions,
            Clock clock) {
        this.books = books;
        this.copies = copies;
        this.members = members;
        this.loans = loans;
        this.reservations = reservations;
        this.properties = properties;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (properties.seedDemoData()) {
            transactions.executeWithoutResult(status -> {
                if (books.count() == 0) {
                    seed();
                }
            });
        }
    }

    private void seed() {
        LocalDate today = LocalDate.now(clock);

        book("Things Fall Apart", "Chinua Achebe", "9780385474542", 1958, "Classics", 3);
        book("Americanah", "Chimamanda Ngozi Adichie", "9780307455925", 2013, "Fiction", 3);
        book("Half of a Yellow Sun", "Chimamanda Ngozi Adichie", "9781400095209", 2006, "Fiction", 2);
        book("Purple Hibiscus", "Chimamanda Ngozi Adichie", "9781616202415", 2003, "Fiction", 2);
        book("A Grain of Wheat", "Ngugi wa Thiong'o", "9780143106692", 1967, "Fiction", 2);
        book("Anthills of the Savannah", "Chinua Achebe", "9780385260459", 1987, "Fiction", 1);
        book("Becoming", "Michelle Obama", "9781524763138", 2018, "Memoir", 2);
        book("Born a Crime", "Trevor Noah", "9780399588174", 2016, "Memoir", 2);
        book("A Brief History of Time", "Stephen Hawking", "9780553380163", 1988, "Science", 1);
        book("Circe", "Madeline Miller", "9780316556347", 2018, "Fiction", 2);
        book("Dune", "Frank Herbert", "9780441172719", 1965, "Science Fiction", 2);
        book("Educated", "Tara Westover", "9780399590504", 2018, "Memoir", 1);
        book("Emma", "Jane Austen", "9780141439587", 1815, "Classics", 1);
        book("Freshwater", "Akwaeke Emezi", "9780802127358", 2018, "Fiction", 1);
        book("Great Expectations", "Charles Dickens", "9780141439563", 1861, "Classics", 2);
        book("Homegoing", "Yaa Gyasi", "9781101971062", 2016, "Fiction", 2);
        book("Kindred", "Octavia E. Butler", "9780807083697", 1979, "Science Fiction", 1);
        book("Little Women", "Louisa May Alcott", "9780147514011", 1868, "Classics", 1);
        book("Matilda", "Roald Dahl", "9780142410370", 1988, "Children", 3);
        book("Middlemarch", "George Eliot", "9780141439549", 1871, "Classics", 1);
        book("My Sister, the Serial Killer", "Oyinkan Braithwaite", "9780525564201", 2018, "Fiction", 2);
        book("Nervous Conditions", "Tsitsi Dangarembga", "9781580050906", 1988, "Fiction", 1);
        book("Oliver Twist", "Charles Dickens", "9780141439747", 1838, "Classics", 1);
        book("Persuasion", "Jane Austen", "9780141439686", 1817, "Classics", 1);
        book("Pride and Prejudice", "Jane Austen", "9780141439518", 1813, "Classics", 2);
        book("Sapiens", "Yuval Noah Harari", "9780062316097", 2011, "History", 2);
        book("Stay With Me", "Ayobami Adebayo", "9780451494603", 2017, "Fiction", 1);
        book("The Famished Road", "Ben Okri", "9780385425131", 1991, "Fiction", 1);
        book("The Hobbit", "J.R.R. Tolkien", "9780547928227", 1937, "Fantasy", 3);
        book("The Joys of Motherhood", "Buchi Emecheta", "9780807616239", 1979, "Fiction", 1);
        book("The Secret Lives of Baba Segi's Wives", "Lola Shoneyin", "9780061946370", 2010, "Fiction", 1);
        book("The Thursday Murder Club", "Richard Osman", "9781984880963", 2020, "Mystery", 2);
        book("Wuthering Heights", "Emily Bronte", "9780141439556", 1847, "Classics", 1);
        book("Zahrah the Windseeker", "Nnedi Okorafor", "9780618926978", 2005, "Children", 1);

        member("Adaeze Okafor", "adaeze.okafor@example.com", "+234 803 555 0142", today.minusMonths(14));
        member("Tunde Bakare", "tunde.bakare@example.com", "+234 802 555 0177", today.minusMonths(9));
        member("Grace Mensah", "grace.mensah@example.com", null, today.minusMonths(7));
        member("Samuel Otieno", "samuel.otieno@example.com", "+254 712 555 019", today.minusMonths(5));
        member("Ifeoma Nwosu", "ifeoma.nwosu@example.com", "+234 809 555 0110", today.minusMonths(4));
        member("Kwame Asante", "kwame.asante@example.com", null, today.minusMonths(3));
        member("Ruth Wanjiru", "ruth.wanjiru@example.com", "+254 722 555 031", today.minusMonths(2));
        member("David Eze", "david.eze@example.com", null, today.minusWeeks(3));

        returned("Adaeze Okafor", "Circe", 0, today.minusDays(40), today.minusDays(27), 0);
        returned("David Eze", "Emma", 0, today.minusDays(20), today.minusDays(4), 0.30);

        open("Tunde Bakare", "Things Fall Apart", 0, today.minusDays(54));
        open("Grace Mensah", "Purple Hibiscus", 0, today.minusDays(17));
        open("Kwame Asante", "Dune", 0, today.minusDays(22));
        open("Adaeze Okafor", "Americanah", 0, today.minusDays(5));
        open("Adaeze Okafor", "The Hobbit", 0, today.minusDays(2));
        open("Samuel Otieno", "Half of a Yellow Sun", 0, today.minusDays(10));
        open("Ifeoma Nwosu", "Half of a Yellow Sun", 1, today.minusDays(12));
        open("Ruth Wanjiru", "Matilda", 0, today.minusDays(1));

        reserve("Kwame Asante", "Half of a Yellow Sun", 1, today.minusDays(6));
        reserve("Ruth Wanjiru", "Half of a Yellow Sun", 2, today.minusDays(3));
    }

    private void book(String title, String author, String isbn, int year, String genre, int copyCount) {
        Book book = books.save(new Book(title, author, isbn, year, genre));
        List<Copy> saved = new ArrayList<>();
        for (int i = 1; i <= copyCount; i++) {
            String barcode = "3%04d%02d".formatted(book.getId(), i);
            saved.add(copies.save(new Copy(book, barcode)));
        }
        copiesByTitle.put(title, saved);
    }

    private void member(String name, String email, String phone, LocalDate joinedOn) {
        String card = "LS-%05d".formatted(members.count() + 10001);
        membersByName.put(name, members.save(new Member(name, email, phone, card, joinedOn)));
    }

    private void open(String memberName, String title, int copyIndex, LocalDate borrowedOn) {
        Copy copy = copiesByTitle.get(title).get(copyIndex);
        copy.setStatus(CopyStatus.ON_LOAN);
        loans.save(new Loan(copy, membersByName.get(memberName), borrowedOn, borrowedOn.plusDays(properties.loanDays())));
    }

    private void returned(String memberName, String title, int copyIndex, LocalDate borrowedOn, LocalDate returnedOn,
            double fee) {
        Copy copy = copiesByTitle.get(title).get(copyIndex);
        Loan loan = new Loan(copy, membersByName.get(memberName), borrowedOn, borrowedOn.plusDays(properties.loanDays()));
        loan.markReturned(returnedOn, fee);
        loans.save(loan);
    }

    private void reserve(String memberName, String title, int position, LocalDate on) {
        Book book = copiesByTitle.get(title).getFirst().getBook();
        reservations.save(new Reservation(book, membersByName.get(memberName), on.atTime(10, 30), position));
    }
}
