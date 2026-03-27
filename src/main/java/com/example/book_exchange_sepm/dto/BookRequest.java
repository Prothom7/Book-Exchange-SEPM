package com.example.book_exchange_sepm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    @NotBlank(message = "Genre is required")
    private String genre;

    private String description;

    private String imageUrl;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    @NotBlank(message = "Condition is required")
    private String condition;

    public BookRequest(String title, String author, String description, String isbn, String condition) {
        this.title = title;
        this.author = author;
        this.genre = "General";
        this.description = description;
        this.isbn = isbn;
        this.condition = condition;
    }
}
