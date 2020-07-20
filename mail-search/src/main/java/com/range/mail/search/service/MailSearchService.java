package com.range.mail.search.service;

import com.range.mail.search.vo.SearchParam;
import com.range.mail.search.vo.SearchResult;


public interface MailSearchService {
    SearchResult search(SearchParam searchParam);
}
