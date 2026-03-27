package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByOwner(User owner);

    List<Book> findByAvailableTrue();

    List<Book> findAllByOrderByTitleAsc();

    @Query("select distinct b.genre from Book b where b.genre is not null order by b.genre asc")
    List<String> findDistinctGenres();

    @Query("select distinct b.language from Book b where b.language is not null order by b.language asc")
    List<String> findDistinctLanguages();
}
