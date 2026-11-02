# Lendshelf

Lendshelf runs the lending desk of a small community library: the public catalogue,
copies on the shelf, member cards, loans and returns, renewals, late fees, the
reservations queue and a dashboard for librarians to chase overdue books.

It is a server-rendered Spring Boot app. There is no login yet; the branch runs it on the
desk computers and the reading-room kiosk.

## Stack

- Java 21, Spring Boot 3.5 (web, Thymeleaf, Data JPA, validation, cache)
- H2 in-memory database, recreated and seeded with demo data on every start
- Maven for builds and tests (JUnit 5, Spring Boot test)

## Running it

With Docker:

```bash
docker compose up --build
```

Natively (JDK 21 and Maven 3.9+):

```bash
mvn spring-boot:run
```

Then open http://localhost:5108. Set `PORT` to use a different port.

## Tests

```bash
mvn test
```

## Where things live

| Path | What |
| --- | --- |
| `src/main/java/com/lendshelf/domain` | JPA entities: `Book`, `Copy`, `Member`, `Loan`, `Reservation` |
| `src/main/java/com/lendshelf/repository` | Spring Data repositories |
| `src/main/java/com/lendshelf/service` | Lending rules: `LoanService`, `ReservationService`, `CatalogService`, `FeeCalculator` |
| `src/main/java/com/lendshelf/web` | MVC controllers and form objects |
| `src/main/java/com/lendshelf/seed` | Demo data loaded at start-up |
| `src/main/resources/templates` | Thymeleaf pages |
| `src/main/resources/application.properties` | Lending rules (loan length, fee per day, renewals, page size) |

## Pages

- `/books`: catalogue with search and paging; `/books/{id}` shows copies and lets the desk lend or reserve
- `/members`: member list and registration; `/members/{id}` is the member's loans, reservations and fees
- `/desk`: circulation desk mode, start with a library card, then scan copy barcodes
- `/librarian`: overdue items and branch numbers

## Lending rules

Configured in `application.properties` under `lendshelf.*`:

- loans last 14 days and can be renewed twice, unless someone is waiting for the book
- late fees are 0.15 per day, with no grace period
- members can have up to 5 books out
- a returned copy is held for the first person in the reservations queue

Demo members have card numbers `LS-10001` to `LS-10008`.
