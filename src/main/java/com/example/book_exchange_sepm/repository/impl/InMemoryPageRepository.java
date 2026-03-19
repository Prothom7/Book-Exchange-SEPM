package com.example.book_exchange_sepm.repository.impl;

import com.example.book_exchange_sepm.model.NavItem;
import com.example.book_exchange_sepm.model.PageContent;
import com.example.book_exchange_sepm.repository.PageRepository;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class InMemoryPageRepository implements PageRepository {

    private static final List<NavItem> NAV_ITEMS = List.of(
            new NavItem("Landing Page", "/landingpage"),
            new NavItem("Browse", "/browse"),
            new NavItem("Book", "/book"),
            new NavItem("Profile", "/profile")
    );

    private static final Map<String, PageContent> PAGE_CONTENTS = createPageContents();

    @Override
    public List<NavItem> findNavigation() {
        return NAV_ITEMS;
    }

    @Override
    public PageContent findPageContent(String pageKey) {
        return PAGE_CONTENTS.getOrDefault(
                pageKey,
                new PageContent(
                        "Book Exchange",
                        "Welcome to Book Exchange",
                        "A simple place to share books with the community.",
                        "/landingpage"
                )
        );
    }

    private static Map<String, PageContent> createPageContents() {
        Map<String, PageContent> pageContents = new LinkedHashMap<>();
        pageContents.put(
                "landingpage",
                new PageContent(
                        "Book Exchange | Landing Page",
                        "Share, Swap, and Discover Books",
                        "A clean starting point for your book-sharing website.",
                        "/landingpage"
                )
        );
        pageContents.put(
                "browse",
                new PageContent(
                        "Book Exchange | Browse",
                        "Browse Available Books",
                        "Find books shared by other readers and pick what you like.",
                        "/browse"
                )
        );
        pageContents.put(
                "book",
                new PageContent(
                        "Book Exchange | Book",
                        "Share a Book",
                        "List a book you want to exchange with the community.",
                        "/book"
                )
        );
        pageContents.put(
                "profile",
                new PageContent(
                        "Book Exchange | Profile",
                        "Your Reader Profile",
                        "Keep your account details and activity in one place.",
                        "/profile"
                )
        );
        return pageContents;
    }
}
