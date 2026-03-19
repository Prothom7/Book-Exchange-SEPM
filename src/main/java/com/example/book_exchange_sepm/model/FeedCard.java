package com.example.book_exchange_sepm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "feed_cards")
public class FeedCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedCardType type;

    @Column(nullable = false)
    private String headline;

    @Column(nullable = false, length = 500)
    private String shortText;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String readingTime;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean active = true;

    public FeedCard() {
    }

    public FeedCard(FeedCardType type,
                    String headline,
                    String shortText,
                    String imageUrl,
                    String readingTime,
                    Integer displayOrder,
                    Boolean active) {
        this.type = type;
        this.headline = headline;
        this.shortText = shortText;
        this.imageUrl = imageUrl;
        this.readingTime = readingTime;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public FeedCardType getType() {
        return type;
    }

    public void setType(FeedCardType type) {
        this.type = type;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getReadingTime() {
        return readingTime;
    }

    public void setReadingTime(String readingTime) {
        this.readingTime = readingTime;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
