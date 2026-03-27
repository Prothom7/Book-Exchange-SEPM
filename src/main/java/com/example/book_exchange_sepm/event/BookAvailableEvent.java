package com.example.book_exchange_sepm.event;

public class BookAvailableEvent {

    private final Long bookId;
    private final String title;
    private final String author;
    private final String genre;

    public BookAvailableEvent(Long bookId, String title, String author, String genre) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.genre = genre;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getGenre() {
        return genre;
    }
}
