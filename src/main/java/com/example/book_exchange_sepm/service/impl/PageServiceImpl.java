package com.example.book_exchange_sepm.service.impl;

import com.example.book_exchange_sepm.model.NavItem;
import com.example.book_exchange_sepm.model.PageContent;
import com.example.book_exchange_sepm.repository.PageRepository;
import com.example.book_exchange_sepm.service.PageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;

    public PageServiceImpl(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Override
    public List<NavItem> getNavigation() {
        return pageRepository.findNavigation();
    }

    @Override
    public PageContent getPageContent(String pageKey) {
        return pageRepository.findPageContent(pageKey);
    }
}
