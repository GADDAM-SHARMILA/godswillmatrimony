package com.godswill.matrimony.config;

import com.godswill.matrimony.model.CarouselImage;
import com.godswill.matrimony.repository.CarouselImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarouselDataInitializer implements CommandLineRunner {

    private final CarouselImageRepository carouselImageRepository;

    @Override
    public void run(String... args) {

        // ✅ Seed only when empty (don't wipe DB on every restart)
        if (carouselImageRepository.count() > 0) {
            System.out.println("⏭ Carousel images already exist. Skipping...");
            return;
        }

        System.out.println("🔄 Initializing carousel images...");

        carouselImageRepository.save(new CarouselImage(
                "/images/Carousel121.png",
                "Find Your Perfect Match",
                "Thousands of verified profiles waiting to connect with you",
                1,
                "center top"  // ✅ Couple at top-center
        ));

        carouselImageRepository.save(new CarouselImage(
                "/images/carousel2.jpg",
                "Trusted & Secure Platform",
                "Safe and secure matrimonial platform for Indian families",
                2,
                "center"  // ✅ Centered
        ));

        carouselImageRepository.save(new CarouselImage(
                "/images/carousel9.jpg",
                "Success Stories",
                "Join thousands of happy couples who found their soulmate",
                3,
                "center top"  // ✅ Mountain landscape at top
        ));

        carouselImageRepository.save(new CarouselImage(
                "/images/carousel4.jpg",
                "Start Your Journey Today",
                "Register free and begin your search for the perfect partner",
                4,
                "center bottom"  // ✅ Ring/hands at bottom
        ));

        carouselImageRepository.save(new CarouselImage(
                "/images/Carousel11.jpg",
                "Real Connections",
                "Meet genuine people with shared values and interests",
                5,
                "center"  // ✅ Centered
        ));

        carouselImageRepository.save(new CarouselImage(
                "/images/Carousel13.jpg",
                "Your Story Begins Here",
                "Find your destiny with our trusted matrimony platform",
                6,
                "right bottom"  // ✅ Couple at right-bottom
        ));

        System.out.println("✅ Carousel images initialized successfully!");
    }
}