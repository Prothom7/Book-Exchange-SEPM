package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.model.NavItem;
import com.example.book_exchange_sepm.model.PageContent;

import java.util.List;

public interface PageService {

    List<NavItem> getNavigation();

    PageContent getPageContent(String pageKey);
}
