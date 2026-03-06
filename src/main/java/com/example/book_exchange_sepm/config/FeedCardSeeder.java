package com.example.book_exchange_sepm.config;

import com.example.book_exchange_sepm.model.FeedCard;
import com.example.book_exchange_sepm.model.FeedCardType;
import com.example.book_exchange_sepm.repository.FeedCardRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class FeedCardSeeder {

    @Bean
    CommandLineRunner seedFeedCards(FeedCardRepository feedCardRepository) {
        return args -> {
            if (feedCardRepository.count() == 0) {
                List<FeedCard> cards = List.of(
                        new FeedCard(
                                FeedCardType.NEWS,
                                "Exchange Drive Starts This Week",
                                "Book drop points are now open in all departments for the spring exchange campaign.",
                                "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=1200&q=80",
                                "3 min read",
                                1,
                                true
                        ),
                        new FeedCard(
                                FeedCardType.NEWS,
                                "Readers Meetup on Friday",
                                "Join a short evening meetup to discuss recent swaps and top community picks.",
                                "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=1200&q=80",
                                "2 min read",
                                2,
                                true
                        ),
                        new FeedCard(
                                FeedCardType.NEWS,
                                "New Genres Added",
                                "Children, science, and productivity categories are now available in browse filters.",
                                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=1200&q=80",
                                "2 min read",
                                3,
                                true
                        ),
                        new FeedCard(
                                FeedCardType.BOOK,
                                "Clean Code",
                                "A practical guide to writing readable, maintainable software in day-to-day projects.",
                                "https://images.unsplash.com/photo-1507842217343-583bb7270b66?auto=format&fit=crop&w=1200&q=80",
                                "4 min read",
                                1,
                                true
                        ),
                        new FeedCard(
                                FeedCardType.BOOK,
                                "The Pragmatic Programmer",
                                "Essential development habits and practical thinking patterns for better engineering.",
                                "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=1200&q=80",
                                "3 min read",
                                2,
                                true
                        ),
                        new FeedCard(
                                FeedCardType.BOOK,
                                "Atomic Habits",
                                "Small daily changes, structured systems, and long-term mindset shifts for progress.",
                                "https://images.unsplash.com/photo-1476275466078-4007374efbbe?auto=format&fit=crop&w=1200&q=80",
                                "3 min read",
                                3,
                                true
                        ),
                        new FeedCard(
                                FeedCardType.AUTHOR,
                                "James Clear",
                                "Focuses on habit design and behavior frameworks that support long-term growth.",
                                "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=1200&q=80",
                                "2 min read",
                                1,
                                true
                        ),
                        new FeedCard(
                                FeedCardType.AUTHOR,
                                "Paulo Coelho",
                                "Known for reflective narratives and clear, philosophical storytelling style.",
                                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=1200&q=80",
                                "2 min read",
                                2,
                                true
                        ),
                        new FeedCard(
                                FeedCardType.AUTHOR,
                                "Cal Newport",
                                "Writes on deep work, digital focus, and building intentional professional routines.",
                                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=1200&q=80",
                                "3 min read",
                                3,
                                true
                        )
                );
                feedCardRepository.saveAll(cards);
            }
        };
    }
}
