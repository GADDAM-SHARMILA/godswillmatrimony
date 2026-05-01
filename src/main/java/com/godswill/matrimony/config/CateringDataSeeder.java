package com.godswill.matrimony.config;

import com.godswill.matrimony.model.CateringItem;
import com.godswill.matrimony.repository.CateringItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CateringDataSeeder implements CommandLineRunner {

    private final CateringItemRepository cateringItemRepository;

    @Override
    public void run(String... args) {

        List<CateringItem> incoming = buildItems();

        // ── Single query: load ALL existing items into memory map (no per-item queries) ──
        Map<String, CateringItem> dbMap = cateringItemRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        i -> i.getName() + "||" + i.getCategory(),
                        i -> i
                ));

        List<CateringItem> toSave = new ArrayList<>();
        int inserted = 0, updated = 0, skipped = 0;

        for (CateringItem item : incoming) {
            String key = item.getName() + "||" + item.getCategory();
            CateringItem db = dbMap.get(key);

            if (db == null) {
                // ── New item → insert ──
                item.onCreate();
                toSave.add(item);
                inserted++;
            } else {
                boolean changed = !db.getDescription().equals(item.getDescription())
                        || !db.getOrigin().equals(item.getOrigin())
                        || db.getDisplayOrder() != item.getDisplayOrder();

                if (changed) {
                    // ── Changed item → update ──
                    db.setDescription(item.getDescription());
                    db.setOrigin(item.getOrigin());
                    db.setDisplayOrder(item.getDisplayOrder());
                    db.setActive(true);
                    db.setUpdatedAt(LocalDateTime.now());
                    toSave.add(db);
                    System.out.printf("🔄 Updated catering item: [%s] %s%n", item.getCategory(), item.getName());
                    updated++;
                } else {
                    skipped++;
                }
            }
        }

        // ── Batch save all inserts + updates in one round trip ──
        if (!toSave.isEmpty()) {
            cateringItemRepository.saveAll(toSave);
        }

        System.out.printf(
                "✅ Catering seed — %d inserted, %d updated, %d unchanged.%n",
                inserted, updated, skipped);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  MASTER ITEM LIST  (105 items — synced with Food Catalogue HTML)
    // ──────────────────────────────────────────────────────────────────────────
    private List<CateringItem> buildItems() {
        return List.of(

                // ══════════════════════════════
                //  BIRYANI (15)
                // ══════════════════════════════
                item("Hyderabadi Dum Biryani", "Biryani",
                        "Slow-cooked basmati rice layered with marinated meat, saffron, fried onions, and authentic spices for a rich flavour and royal aroma.",
                        "Hyderabad", 1),

                item("Chicken 65 Biryani", "Biryani",
                        "A fiery biryani made with crispy Chicken 65 pieces, fragrant rice, fresh herbs, and bold masala for a spicy and tangy finish.",
                        "Hyderabad", 2),

                item("Mutton Ghee Roast Biryani", "Biryani",
                        "Tender mutton cooked in ghee-roasted spices and layered with aromatic rice for a deep, peppery, and indulgent taste.",
                        "South India", 3),

                item("Kalyani Biryani", "Biryani",
                        "A rustic and robust biryani known for its hearty masala, comforting aroma, and balanced spice profile with a homestyle character.",
                        "Hyderabad", 4),

                item("Lucknowi Awadhi Biryani", "Biryani",
                        "Delicately spiced rice and meat cooked in the dum style, celebrated for its subtle aroma, elegance, and refined flavour.",
                        "Lucknow", 5),

                item("Kolkata Biryani", "Biryani",
                        "A lighter biryani with soft potatoes, boiled egg, mellow spices, and aromatic rice that delivers gentle fragrance and comfort.",
                        "Kolkata", 6),

                item("Ambur Biryani", "Biryani",
                        "Prepared with seeraga samba rice and a peppery masala, this biryani is moist, flavour-packed, and wonderfully aromatic.",
                        "Ambur, Tamil Nadu", 7),

                item("Thalassery Biryani", "Biryani",
                        "A Malabar classic featuring fragrant rice, dry fruits, gentle spices, and rich layers of flavour with a festive aroma.",
                        "Thalassery, Kerala", 8),

                item("Paneer Tikka Biryani", "Biryani",
                        "Smoky paneer tikka cubes layered with saffron rice, mint, and spiced gravy for a vegetarian biryani full of colour and aroma.",
                        "North India", 9),

                item("Mushroom Biryani", "Biryani",
                        "Juicy mushrooms cooked with herbs, whole spices, and long-grain rice to create a savoury biryani with earthy fragrance.",
                        "South India", 10),

                // ── 5 new biryani items ──
                item("Prawn Biryani", "Biryani",
                        "Succulent prawns layered with fragrant rice, coastal spices, and herbs for a seafood biryani with fresh aroma and delicate heat.",
                        "Andhra Coast", 11),

                item("Fish Biryani", "Biryani",
                        "Tender fish pieces cooked with spiced rice, caramelised onions, and herbs for a light yet flavourful coastal biryani.",
                        "Malabar, Kerala", 12),

                item("Ulavacharu Chicken Biryani", "Biryani",
                        "A special biryani infused with horse gram sauce, juicy chicken, and aromatic rice for a tangy, rich, and deeply savoury taste.",
                        "Guntur, Andhra Pradesh", 13),

                item("Donne Biryani", "Biryani",
                        "A Karnataka favourite made with short-grain rice, green masala, and robust spices, served with a signature peppery aroma.",
                        "Bengaluru", 14),

                item("Egg Biryani", "Biryani",
                        "Boiled eggs layered with spiced rice, herbs, and caramelised onions for a simple, comforting biryani full of homestyle flavour.",
                        "Indian Home Style", 15),

                // ══════════════════════════════
                //  FRIED RICE & NOODLES (15)
                // ══════════════════════════════
                item("Veg Fried Rice", "Fried Rice & Noodles",
                        "Wok-tossed rice with colourful vegetables, spring onion, light soy notes, and a smoky street-style finish.",
                        "Indo-Chinese", 16),

                item("Schezwan Fried Rice", "Fried Rice & Noodles",
                        "A spicy fried rice infused with schezwan sauce, garlic, crunchy vegetables, and a bold chilli kick.",
                        "Indo-Chinese", 17),

                item("Egg Fried Rice", "Fried Rice & Noodles",
                        "Fluffy rice stir-fried with scrambled egg, pepper, spring onions, and savoury sauces for a comforting classic.",
                        "Chinese-inspired", 18),

                item("Chicken Fried Rice", "Fried Rice & Noodles",
                        "Tender chicken, seasoned rice, and vegetables tossed over high heat to deliver smoky flavour and satisfying texture.",
                        "Indo-Chinese", 19),

                item("Paneer Fried Rice", "Fried Rice & Noodles",
                        "Cubes of paneer tossed with rice, vegetables, and mild spices to create a rich and filling fusion favourite.",
                        "Indian Fusion", 20),

                item("Hakka Noodles", "Fried Rice & Noodles",
                        "Classic stir-fried noodles with cabbage, capsicum, and spring onions for a light, savoury, and slurp-worthy dish.",
                        "Indo-Chinese, Kolkata Chinatown", 21),

                item("Schezwan Noodles", "Fried Rice & Noodles",
                        "Fiery noodles tossed in spicy schezwan sauce with crisp vegetables for a hot and flavourful bite.",
                        "Indo-Chinese", 22),

                item("Garlic Noodles", "Fried Rice & Noodles",
                        "Silky noodles coated in buttery garlic seasoning and herbs for a simple yet aromatic comfort dish.",
                        "Asian Fusion", 23),

                item("Singapore Noodles", "Fried Rice & Noodles",
                        "Thin noodles tossed with curry seasoning, vegetables, and lively spices for a bright golden finish.",
                        "South-East Asian style", 24),

                item("Chilli Garlic Noodles", "Fried Rice & Noodles",
                        "A bold noodle dish with crushed chilli, roasted garlic, and wok aroma for a spicy, punchy flavour.",
                        "Indo-Chinese", 25),

                // ── 5 new fried rice & noodle items ──
                item("Mushroom Fried Rice", "Fried Rice & Noodles",
                        "Rice stir-fried with mushrooms, spring onions, and sauces for an earthy, smoky, and satisfying bowl.",
                        "Indo-Chinese", 26),

                item("Prawn Fried Rice", "Fried Rice & Noodles",
                        "Juicy prawns tossed with seasoned rice, vegetables, and pepper for a seafood favourite with wok aroma.",
                        "Coastal Indo-Chinese", 27),

                item("Burnt Garlic Fried Rice", "Fried Rice & Noodles",
                        "Fragrant fried rice accented with crispy burnt garlic for a rich savoury taste and irresistible aroma.",
                        "Asian Fusion", 28),

                item("Triple Schezwan Rice", "Fried Rice & Noodles",
                        "A street-style combo of fried rice, noodles, spicy gravy, and crunchy toppings delivering heat, texture, and bold flavour.",
                        "Mumbai Indo-Chinese", 29),

                item("American Chopsuey", "Fried Rice & Noodles",
                        "Crispy noodles topped with sweet-spicy vegetable sauce for a crunchy, tangy, and colourful Indo-Chinese favourite.",
                        "Indo-Chinese", 30),

                // ══════════════════════════════
                //  ROTI & BREAD (15)
                // ══════════════════════════════
                item("Tandoori Roti", "Roti & Bread",
                        "Whole wheat flatbread baked in a tandoor for a rustic texture, smoky aroma, and wholesome flavour.",
                        "North India", 31),

                item("Butter Naan", "Roti & Bread",
                        "Soft and fluffy leavened bread brushed with butter, perfect for pairing with rich curries and gravies.",
                        "Punjab", 32),

                item("Garlic Naan", "Roti & Bread",
                        "Classic naan topped with garlic and butter, offering a fragrant bite with every tear and dip.",
                        "North India", 33),

                item("Lachha Paratha", "Roti & Bread",
                        "A flaky, layered paratha cooked to golden perfection with a crisp outside and soft inside.",
                        "Punjab", 34),

                item("Rumali Roti", "Roti & Bread",
                        "Paper-thin soft bread folded like a handkerchief, ideal with kebabs and creamy curries.",
                        "Mughlai Cuisine", 35),

                item("Missi Roti", "Roti & Bread",
                        "A hearty flatbread made with gram flour, spices, and herbs, delivering earthy flavour and nutrition.",
                        "Punjab", 36),

                item("Amritsari Kulcha", "Roti & Bread",
                        "Stuffed bread baked until crisp and golden, known for its rich filling and irresistible tandoor aroma.",
                        "Amritsar", 37),

                item("Malabar Parotta", "Roti & Bread",
                        "Soft, layered, and flaky flatbread that pairs beautifully with spicy salna and South Indian curries.",
                        "Kerala", 38),

                item("Phulka", "Roti & Bread",
                        "A simple puffed whole wheat bread that is light, healthy, and a staple on traditional Indian plates.",
                        "Indian Home Kitchens", 39),

                item("Stuffed Paneer Paratha", "Roti & Bread",
                        "A pan-roasted flatbread stuffed with spiced paneer, offering a rich, soft, and satisfying bite.",
                        "North India", 40),

                // ── 5 new bread items ──
                item("Butter Roti", "Roti & Bread",
                        "Soft whole wheat roti brushed with butter for a warm, simple, and comforting accompaniment to curries.",
                        "Indian Home Style", 41),

                item("Plain Naan", "Roti & Bread",
                        "Classic soft naan with a light tandoor char, ideal for scooping up rich gravies and creamy curries.",
                        "North India", 42),

                item("Pudina Paratha", "Roti & Bread",
                        "A flavourful paratha infused with mint and spices, delivering freshness and gentle layers in every bite.",
                        "North India", 43),

                item("Keema Naan", "Roti & Bread",
                        "A stuffed naan filled with spiced minced meat, baked until soft inside and lightly crisp outside.",
                        "Mughlai Cuisine", 44),

                item("Cheese Kulcha", "Roti & Bread",
                        "A stuffed kulcha with gooey cheese and soft bread layers, baked for a rich and indulgent flavour.",
                        "Amritsar", 45),

                // ══════════════════════════════
                //  SNACKS (15)
                // ══════════════════════════════
                item("Samosa", "Snacks",
                        "Golden pastry stuffed with spiced potatoes and peas, loved for its crisp shell and savoury filling.",
                        "North India", 46),

                item("Kachori", "Snacks",
                        "A flaky deep-fried snack with a spiced lentil filling that delivers crunch and bold flavour.",
                        "Rajasthan", 47),

                item("Pav Bhaji", "Snacks",
                        "Mashed vegetable bhaji cooked with butter and spices, served with toasted pav for a hearty street-food treat.",
                        "Mumbai", 48),

                item("Dahi Puri", "Snacks",
                        "Crispy puris filled with chutneys, curd, and masala for a burst of sweet, spicy, and tangy flavours.",
                        "Mumbai", 49),

                item("Aloo Tikki Chaat", "Snacks",
                        "Crisp potato patties topped with curd, chutneys, and sev for a layered and lively street-style snack.",
                        "Delhi", 50),

                item("Medu Vada", "Snacks",
                        "A crispy lentil doughnut with a soft centre, traditionally served with coconut chutney and sambar.",
                        "South India", 51),

                item("Masala Dosa", "Snacks",
                        "Thin crispy dosa filled with spiced potato masala, known for its buttery aroma and satisfying crunch.",
                        "Karnataka", 52),

                item("Kara Paniyaram", "Snacks",
                        "Soft dumplings made from fermented batter, lightly crisp outside and fluffy inside with tempering flavours.",
                        "Tamil Nadu", 53),

                item("Mirchi Bajji", "Snacks",
                        "Large green chillies dipped in gram flour batter and fried to golden perfection, popular as a spicy tea-time snack.",
                        "Andhra Pradesh", 54),

                item("Punugulu", "Snacks",
                        "Crispy fritters made from dosa batter, served hot with chutney for a light and addictive South Indian snack.",
                        "Andhra Pradesh", 55),

                // ── 5 new snack items ──
                item("Onion Pakoda", "Snacks",
                        "Thin-sliced onions coated in gram flour batter and fried until crisp for the perfect rainy-day snack.",
                        "India", 56),

                item("Bhel Puri", "Snacks",
                        "A lively mix of puffed rice, chutneys, onions, and sev for a crunchy, tangy, and refreshing chaat bite.",
                        "Mumbai", 57),

                item("Idli", "Snacks",
                        "Soft steamed rice cakes served with chutney and sambar, known for their light texture and wholesome taste.",
                        "South India", 58),

                item("Mysore Bonda", "Snacks",
                        "Fluffy fried dumplings with a crisp shell and soft centre, ideal with coconut chutney and tea.",
                        "Karnataka", 59),

                item("Sabudana Vada", "Snacks",
                        "A crispy sago and peanut patty with gentle spice and nutty flavour, popular as a special-occasion snack.",
                        "Maharashtra", 60),

                // ══════════════════════════════
                //  CURRIES (15)
                // ══════════════════════════════
                item("Butter Chicken", "Curries",
                        "Tandoor-cooked chicken simmered in a velvety tomato-butter gravy with a mildly sweet and smoky finish.",
                        "Delhi", 61),

                item("Chicken Tikka Masala", "Curries",
                        "Grilled chicken tikka folded into a creamy masala gravy loaded with onion, tomato, and warming spices.",
                        "North Indian style", 62),

                item("Paneer Butter Masala", "Curries",
                        "Soft paneer cubes cooked in a rich tomato-cashew gravy that is buttery, smooth, and family-friendly.",
                        "Punjab", 63),

                item("Palak Paneer", "Curries",
                        "Fresh spinach puree and paneer simmered with mild spices for a wholesome curry with earthy flavour.",
                        "North India", 64),

                item("Dal Makhani", "Curries",
                        "Black lentils and kidney beans slow-cooked with butter and cream for a smoky, rich, and comforting dish.",
                        "Punjab", 65),

                item("Kadai Chicken", "Curries",
                        "Chicken cooked with capsicum, onion, and crushed spices for a bold, peppery curry with rustic flavour.",
                        "North India", 66),

                item("Mutton Rogan Josh", "Curries",
                        "Aromatic mutton curry with deep red colour, slow-cooked spices, and a rich, warming profile.",
                        "Kashmir", 67),

                item("Chettinad Chicken", "Curries",
                        "A fiery South Indian curry packed with roasted spices, black pepper, and coconut depth.",
                        "Chettinad, Tamil Nadu", 68),

                item("Gongura Mutton", "Curries",
                        "Mutton cooked with tangy gongura leaves and spices for a bold and uniquely Andhra flavour.",
                        "Andhra Pradesh", 69),

                item("Vegetable Kurma", "Curries",
                        "Mixed vegetables simmered in a mildly spiced coconut-based gravy that is creamy, fragrant, and comforting.",
                        "South India", 70),

                // ── 5 new curry items ──
                item("Paneer Tikka Masala", "Curries",
                        "Grilled paneer cubes simmered in a creamy tomato-onion gravy for a smoky and satisfying curry experience.",
                        "North India", 71),

                item("Chana Masala", "Curries",
                        "Chickpeas cooked in a tangy tomato-onion masala with warming spices for a hearty and classic dish.",
                        "Punjab", 72),

                item("Aloo Gobi", "Curries",
                        "Potatoes and cauliflower tossed in dry masala with turmeric and herbs for a simple, homestyle favourite.",
                        "North India", 73),

                item("Egg Curry", "Curries",
                        "Boiled eggs simmered in a robust onion-tomato gravy with spices for a comforting everyday curry.",
                        "Indian Home Style", 74),

                item("Fish Curry", "Curries",
                        "A coastal curry with tender fish, tangy notes, and vibrant spice, perfect with rice or bread.",
                        "Goa", 75),

                // ══════════════════════════════
                //  STARTERS (15)
                // ══════════════════════════════
                item("Chicken 65", "Starters",
                        "Crispy deep-fried chicken tossed with curry leaves, garlic, and spice for a hot and addictive appetiser.",
                        "Hyderabad", 76),

                item("Paneer Tikka", "Starters",
                        "Chunks of paneer marinated in yogurt and spices, then grilled for a smoky and juicy finish.",
                        "Punjab", 77),

                item("Hariyali Kebab", "Starters",
                        "Tender kebabs coated in mint, coriander, and yogurt marinade for a fresh green flavour profile.",
                        "North India", 78),

                item("Malai Kebab", "Starters",
                        "Creamy, mildly spiced kebabs with a soft texture and delicate richness from cheese and cream.",
                        "Mughlai Cuisine", 79),

                item("Chilli Paneer", "Starters",
                        "Crisp paneer cubes tossed with onions, capsicum, and spicy sauces for a popular fusion appetiser.",
                        "Indo-Chinese", 80),

                item("Dragon Chicken", "Starters",
                        "Crispy chicken strips coated in a sweet-spicy sauce with sesame and chilli for a bold crunch.",
                        "Indo-Chinese", 81),

                item("Gobi Manchurian", "Starters",
                        "Crispy cauliflower florets tossed in tangy Manchurian sauce for a flavour-packed vegetarian starter.",
                        "Indo-Chinese", 82),

                item("Tandoori Prawns", "Starters",
                        "Juicy prawns marinated with chilli, lemon, and spices before being char-grilled to perfection.",
                        "Coastal India", 83),

                item("Mutton Seekh Kebab", "Starters",
                        "Minced mutton blended with herbs and spices, skewered and grilled for a succulent smoky appetiser.",
                        "North India", 84),

                item("Crispy Corn", "Starters",
                        "Golden fried corn kernels tossed with chilli, onions, and herbs for a crunchy and crowd-pleasing starter.",
                        "Indian Fusion", 85),

                // ── 5 new starter items ──
                item("Veg Manchurian", "Starters",
                        "Crispy vegetable balls tossed in glossy Manchurian sauce for a tangy, savoury, and crowd-favourite starter.",
                        "Indo-Chinese", 86),

                item("Baby Corn Pepper Fry", "Starters",
                        "Tender baby corn tossed with pepper, curry leaves, and spices for a crisp starter with South Indian flair.",
                        "South India", 87),

                item("Apollo Fish", "Starters",
                        "Boneless fish fried and coated in a spicy creamy masala for a signature Hyderabadi seafood starter.",
                        "Hyderabad", 88),

                item("Chicken Lollipop", "Starters",
                        "Frenched chicken wings marinated and fried until crisp, served with spicy dips for a party-perfect appetiser.",
                        "Indo-Chinese", 89),

                item("Mushroom Pepper Fry", "Starters",
                        "Mushrooms sautéed with black pepper, onions, and herbs for an earthy and aromatic starter.",
                        "South India", 90),

                // ══════════════════════════════
                //  PLATTERS (15)
                // ══════════════════════════════
                item("Veg Mini Platter", "Platters",
                        "A curated selection of crispy and grilled vegetarian starters served with dips for a balanced tasting experience.",
                        "Indian Restaurant Style", 91),

                item("Non-Veg Mini Platter", "Platters",
                        "A flavourful assortment of chicken starters and kebabs designed for sampling multiple favourites in one plate.",
                        "Indian Restaurant Style", 92),

                item("Tandoori Platter", "Platters",
                        "A mixed grill platter featuring tandoori classics with smoky aroma, vibrant colours, and mint chutney on the side.",
                        "North India", 93),

                item("Kebab Platter", "Platters",
                        "An assorted platter of soft, juicy kebabs with contrasting marinades and grilled perfection in every bite.",
                        "Mughlai Cuisine", 94),

                item("Seafood Platter", "Platters",
                        "A coastal spread of prawns, fish, and seafood bites marinated with spices and cooked for fresh ocean flavour.",
                        "Coastal India", 95),

                item("South Indian Breakfast Platter", "Platters",
                        "A wholesome platter featuring dosa, idli, vada, chutneys, and sambar for a traditional South Indian meal experience.",
                        "South India", 96),

                item("North Indian Thali", "Platters",
                        "A complete platter with bread, curry, rice, dal, accompaniments, and dessert for a satisfying royal meal.",
                        "North India", 97),

                item("South Indian Meals Platter", "Platters",
                        "A classic banana-leaf style combination of rice, curry, poriyal, sambar, rasam, and sides in one platter.",
                        "Tamil Nadu", 98),

                item("Family Combo Platter", "Platters",
                        "A generous sharing platter with breads, rice, curries, and starters designed for family-style dining.",
                        "Indian Restaurant Style", 99),

                item("Festival Special Platter", "Platters",
                        "A celebratory assortment featuring festive favourites, rich flavours, and variety curated for special occasions.",
                        "Indian Festive Cuisine", 100),

                // ── 5 new platter items ──
                item("Biryani Feast Platter", "Platters",
                        "A complete platter with signature biryani, salan, raita, kebabs, and accompaniments for a grand dining experience.",
                        "Hyderabad Style", 101),

                item("Vegetarian Deluxe Thali", "Platters",
                        "A premium vegetarian platter featuring paneer, dal, sabzi, bread, rice, dessert, and traditional accompaniments.",
                        "Indian Restaurant Style", 102),

                item("Non-Veg Deluxe Thali", "Platters",
                        "A royal platter with chicken curry, mutton dish, rice, breads, sides, and dessert for a complete feast.",
                        "Indian Restaurant Style", 103),

                item("Street Food Platter", "Platters",
                        "A fun assortment of street favourites like chaat, pav bhaji, samosa, and chutneys bursting with colour and flavour.",
                        "Indian Street Food", 104),

                item("Kids Special Platter", "Platters",
                        "A mild and colourful combo platter with crowd-pleasing bites, dips, rice, and fun textures for younger diners.",
                        "Restaurant Fusion Style", 105)
        );
    }

    // ── Helper to build a CateringItem ──
    private CateringItem item(String name, String category,
                              String description, String origin,
                              int displayOrder) {
        CateringItem i = new CateringItem();
        i.setName(name);
        i.setCategory(category);
        i.setDescription(description);
        i.setOrigin(origin);
        i.setDisplayOrder(displayOrder);
        i.setActive(true);
        i.setImageUrl("");
        return i;
    }
}