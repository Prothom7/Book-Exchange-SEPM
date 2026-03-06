package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.model.NavItem;
import com.example.book_exchange_sepm.model.PageContent;

import java.util.List;

public interface PageRepository {

    List<NavItem> findNavigation();

    PageContent findPageContent(String pageKey);
}
