package com.example.book_exchange_sepm.service.impl;

import com.example.book_exchange_sepm.model.CarouselSlide;
import com.example.book_exchange_sepm.repository.CarouselSlideRepository;
import com.example.book_exchange_sepm.service.CarouselSlideService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarouselSlideServiceImpl implements CarouselSlideService {

    private final CarouselSlideRepository carouselSlideRepository;

    public CarouselSlideServiceImpl(CarouselSlideRepository carouselSlideRepository) {
        this.carouselSlideRepository = carouselSlideRepository;
    }

    @Override
    public List<CarouselSlide> getActiveSlides() {
        return carouselSlideRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public CarouselSlide createSlide(CarouselSlide slide) {
        return carouselSlideRepository.save(slide);
    }
}
