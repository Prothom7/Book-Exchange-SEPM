package com.example.book_exchange_sepm.controller.form;

import com.example.book_exchange_sepm.model.FeedCardType;

public class AdminFeedCardForm {

    private FeedCardType type = FeedCardType.NEWS;
    private String headline;
    private String shortText;
    private String imageUrl;
    private String readingTime;
    private Integer displayOrder;
    private Boolean active = true;

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
