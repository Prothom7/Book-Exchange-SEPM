package com.example.book_exchange_sepm.service.impl;

import com.example.book_exchange_sepm.model.FeedCard;
import com.example.book_exchange_sepm.model.FeedCardType;
import com.example.book_exchange_sepm.repository.FeedCardRepository;
import com.example.book_exchange_sepm.service.FeedCardService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedCardServiceImpl implements FeedCardService {

    private final FeedCardRepository feedCardRepository;

    public FeedCardServiceImpl(FeedCardRepository feedCardRepository) {
        this.feedCardRepository = feedCardRepository;
    }

    @Override
    public List<FeedCard> getActiveCardsByType(FeedCardType type) {
        return feedCardRepository.findByTypeAndActiveTrueOrderByDisplayOrderAsc(type);
    }

    @Override
    public FeedCard createCard(FeedCard card) {
        return feedCardRepository.save(card);
    }
}
