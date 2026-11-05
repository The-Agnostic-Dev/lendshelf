package com.lendshelf.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.lendshelf.service.CatalogService;

@ControllerAdvice(assignableTypes = CatalogController.class)
public class CatalogModelAdvice {

    private final CatalogService catalog;

    public CatalogModelAdvice(CatalogService catalog) {
        this.catalog = catalog;
    }

    @ModelAttribute
    public void addTotalTitles(Model model) {
        model.addAttribute("totalTitles", catalog.totalTitles());
    }
}
