package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.model.CarouselSlide;
import com.example.book_exchange_sepm.repository.CarouselSlideRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CarouselSlideSeeder {

    @Bean
    CommandLineRunner seedCarouselSlides(CarouselSlideRepository carouselSlideRepository) {
        return args -> {
            if (carouselSlideRepository.count() == 0) {
                List<CarouselSlide> slides = List.of(
                        new CarouselSlide(
                                "Share, Swap, and Discover Books",
                                "A clean starting point for your book-sharing website.",
                                "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=1600&q=80",
                                1,
                                true
                        ),
                        new CarouselSlide(
                                "Future Database Image Slot",
                                "This slide is ready for dynamic image data.",
                                "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=1600&q=80",
                                2,
                                true
                        ),
                        new CarouselSlide(
                                "Community Highlights",
                                "Show top shared books or announcements here.",
                                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=1600&q=80",
                                3,
                                true
                        )
                );
                carouselSlideRepository.saveAll(slides);
            }
        };
    }
}
