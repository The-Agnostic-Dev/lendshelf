package com.lendshelf.web;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class Dates {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private Dates() {
    }

    static String display(LocalDate date) {
        return date == null ? "" : DISPLAY.format(date);
    }
}
