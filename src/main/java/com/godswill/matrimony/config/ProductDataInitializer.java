package com.godswill.matrimony.config;

import com.godswill.matrimony.model.Product;
import com.godswill.matrimony.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Seeds initial products into MongoDB on first startup.
 * Runs after your existing DataInitializer (order = 2).
 * Safe to keep — only inserts if products collection is empty.
 */
@Component
@Order(2)
public class ProductDataInitializer implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            System.out.println("Seeding wedding products...");
            productRepository.saveAll(buildProducts());
            System.out.println(productRepository.count() + " products seeded.");
        }
    }

    private List<Product> buildProducts() {

        // ── BRIDE ──────────────────────────────────────────────────────────────
        Product p1 = bride("Luxury Bridal Lehenga",
                "Elegant bridal lehenga with intricate embroidery work.",
                18999d, 15999d, "/images/bridal-lehenga.jpg", 15,
                "S,M,L,XL", "Red,Pink,Maroon", "Lehenga", "Silk", 7);

        Product p2 = bride("Bridal Jewelry Set",
                "Complete set with necklace, earrings, and bangles.",
                12499d, 9999d, "/images/bridal-jewelry.jpg", 20,
                "Free Size", "Gold,Silver", "Jewellery", null, 5);

        Product p3 = bride("Bridal Beauty Kit",
                "Professional bridal beauty kit with all essentials.",
                5999d, (Double) null, "/images/beauty-kit.jpg", 30,
                null, null, "Beauty", null, 3);

        Product p4 = bride("Statement Earrings",
                "Stunning statement earrings for the modern bride.",
                2299d, 1899d, "/images/earrings.jpg", 50,
                "Free Size", "Gold,Rose Gold", "Jewellery", null, 4);

        Product p5 = bride("Designer Bridal Saree",
                "Handwoven bridal saree with golden zari work.",
                16499d, 13999d, "/images/bridal-saree.jpg", 10,
                "Free Size", "Red,Cream,Peach", "Saree", "Silk", 7);

        Product p6 = bride("Maang Tikka Set",
                "Traditional maang tikka set with kundan stone work.",
                8999d, 7499d, "/images/maang-tikka.jpg", 25,
                "Free Size", "Gold,Antique Gold", "Jewellery", null, 5);

        Product p7 = bride("Bridal Skincare Set",
                "Premium skincare set for glowing bridal skin.",
                3799d, (Double) null, "/images/skincare.jpg", 40,
                null, null, "Beauty", null, 3);

        Product p8 = bride("Hair Accessories Combo",
                "Beautiful bridal hair accessories combo set.",
                2599d, 1999d, "/images/hair-accessories.jpg", 35,
                "Free Size", "Gold,Silver,Pearl", "Accessories", null, 4);

        // ── GROOM ──────────────────────────────────────────────────────────────
        Product p9 = groom("Premium Wedding Suit",
                "Tailored premium suit in finest wool blend.",
                14999d, 12499d, "/images/wedding-suit.jpg", 20,
                "38,40,42,44,46", "Black,Navy Blue,Charcoal Grey", "Suit", "Wool Blend", 7);

        Product p10 = groom("Groom Formal Shoes",
                "Premium leather formal shoes for the groom.",
                4599d, 3799d, "/images/groom-shoes.jpg", 30,
                "7,8,9,10,11", "Black,Brown,Tan", "Footwear", "Genuine Leather", 5);

        Product p11 = groom("Groom Cufflinks and Tie",
                "Elegant cufflinks and tie combo.",
                2899d, 2299d, "/images/cufflinks-tie.jpg", 50,
                "Free Size", "Silver,Gold,Rose Gold", "Accessories", null, 4);

        Product p12 = groom("Groom Grooming Kit",
                "Complete grooming kit for the perfect groom look.",
                3499d, (Double) null, "/images/grooming-kit.jpg", 25,
                null, null, "Grooming", null, 3);

        Product p13 = groom("Groom Premium Watch",
                "Luxury timepiece to complement the wedding attire.",
                8999d, 7499d, "/images/groom-watch.jpg", 15,
                "Free Size", "Silver,Gold,Black", "Accessories", null, 5);

        Product p14 = groom("Tie and Pocket Square Set",
                "Matching tie and pocket square set.",
                1999d, 1599d, "/images/tie-pocket.jpg", 60,
                "Free Size", "Maroon,Navy,Black,Champagne", "Accessories", null, 4);

        Product p15 = groom("Premium Wedding Perfume",
                "Long-lasting luxury wedding perfume.",
                2799d, (Double) null, "/images/groom-perfume.jpg", 40,
                null, null, "Grooming", null, 3);

        Product p16 = groom("Belt and Wallet Gift Set",
                "Premium leather belt and wallet gift set.",
                3299d, 2699d, "/images/belt-wallet.jpg", 30,
                "28,30,32,34,36", "Black,Brown,Tan", "Accessories", "Genuine Leather", 5);

        return Arrays.asList(p1, p2, p3, p4, p5, p6, p7, p8,
                p9, p10, p11, p12, p13, p14, p15, p16);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product bride(String name, String desc,
                          double originalPrice, Double discountedPrice,
                          String img, int stock,
                          String sizes, String colors,
                          String subCategory, String material, int deliveryDays) {
        return build(name, desc, originalPrice, discountedPrice,
                "bride", img, stock, sizes, colors, subCategory, material, deliveryDays);
    }

    private Product groom(String name, String desc,
                          double originalPrice, Double discountedPrice,
                          String img, int stock,
                          String sizes, String colors,
                          String subCategory, String material, int deliveryDays) {
        return build(name, desc, originalPrice, discountedPrice,
                "groom", img, stock, sizes, colors, subCategory, material, deliveryDays);
    }

    private Product build(String name, String desc,
                          double originalPrice, Double discountedPrice,
                          String category, String img, int stock,
                          String sizes, String colors,
                          String subCategory, String material, int deliveryDays) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setCategory(category);
        p.setImageUrl(img);
        p.setStock(stock);
        p.setOriginalPrice(originalPrice);
        p.setDiscountedPrice(discountedPrice);
        p.setSizes(sizes);
        p.setColors(colors);
        p.setSubCategory(subCategory);
        p.setMaterial(material);
        p.setDeliveryDays(deliveryDays);
        p.setActive(true);
        p.setFeatured(false);
        return p;
    }
}