package com.example.book_exchange_sepm.pattern.singleton;

import com.example.book_exchange_sepm.event.BookAvailableEvent;

public interface BookAvailabilitySubscriber {

    void onBookAvailable(BookAvailableEvent event);
}
