package com.range.mail.search.controller;

import com.range.mail.search.service.MailSearchService;
import com.range.mail.search.vo.SearchParam;
    import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.range.mail.search.vo.SearchResult;


@RestController
public class SearchController {

    @Autowired
    MailSearchService mailSearchService;

    @GetMapping("/list.html")
    public String listPage(SearchParam searchParam, Model model) {
        SearchResult result = mailSearchService.search(searchParam);
        model.addAttribute("result", result);
        return "list";
    }
}
