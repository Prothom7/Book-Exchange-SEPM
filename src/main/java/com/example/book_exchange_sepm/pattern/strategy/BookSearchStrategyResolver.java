package com.example.book_exchange_sepm.pattern.strategy;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BookSearchStrategyResolver {

    private final Map<SearchMode, BookSearchStrategy> strategies;

    public BookSearchStrategyResolver(KeywordSearchStrategy keywordSearchStrategy,
                                      TitleSearchStrategy titleSearchStrategy,
                                      AuthorSearchStrategy authorSearchStrategy,
                                      GenreSearchStrategy genreSearchStrategy) {
        this.strategies = Map.of(
            SearchMode.KEYWORD, keywordSearchStrategy,
            SearchMode.TITLE, titleSearchStrategy,
            SearchMode.AUTHOR, authorSearchStrategy,
            SearchMode.GENRE, genreSearchStrategy
        );
    }

    public BookSearchStrategy resolve(SearchMode searchMode) {
        return strategies.getOrDefault(searchMode, strategies.get(SearchMode.KEYWORD));
    }
}
