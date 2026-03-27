package com.example.book_exchange_sepm.pattern.singleton;

import com.example.book_exchange_sepm.event.BookAvailableEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BookEventManager {

    private static final BookEventManager INSTANCE = new BookEventManager();

    private final List<BookAvailabilitySubscriber> subscribers = new CopyOnWriteArrayList<>();

    private BookEventManager() {
    }

    public static BookEventManager getInstance() {
        return INSTANCE;
    }

    public void subscribe(BookAvailabilitySubscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void unsubscribe(BookAvailabilitySubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    public void publish(BookAvailableEvent event) {
        for (BookAvailabilitySubscriber subscriber : subscribers) {
            subscriber.onBookAvailable(event);
        }
    }
}
