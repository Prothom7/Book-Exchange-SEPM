package com.example.book_exchange_sepm.repository;

import com.example.book_exchange_sepm.model.CarouselSlide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarouselSlideRepository extends JpaRepository<CarouselSlide, Long> {

    List<CarouselSlide> findByActiveTrueOrderByDisplayOrderAsc();
}
