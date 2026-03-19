package com.example.book_exchange_sepm.service;

import com.example.book_exchange_sepm.model.FeedCard;
import com.example.book_exchange_sepm.model.FeedCardType;

import java.util.List;

public interface FeedCardService {

    List<FeedCard> getActiveCardsByType(FeedCardType type);
}
