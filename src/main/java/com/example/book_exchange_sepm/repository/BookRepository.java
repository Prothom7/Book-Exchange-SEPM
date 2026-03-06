package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.entity.Book;
import com.example.book_exchange_sepm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByOwner(User owner);
    List<Book> findByAvailableTrue();
    List<Book> findAll();
}
