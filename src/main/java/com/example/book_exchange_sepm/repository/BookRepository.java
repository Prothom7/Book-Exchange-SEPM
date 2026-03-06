package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

  List<Book> findAllByOrderByTitleAsc();

    @Query("select distinct b.genre from Book b order by b.genre asc")
    List<String> findDistinctGenres();

    @Query("select distinct b.language from Book b order by b.language asc")
    List<String> findDistinctLanguages();
}
