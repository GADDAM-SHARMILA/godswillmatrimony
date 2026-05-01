package com.godswill.matrimony.model;

public enum SubscriptionPlan {
    BRONZE(299, 1, "1 Month", 0),
    SILVER(599, 3, "3 Months", 33.22),
    GOLD(999, 6, "6 Months", 44.31),
    PLATINUM(1499, 12, "1 Year", 58.22),
    RUBY(2999, 36, "3 Years", 72.14);

    private final int price;
    private final int durationMonths;
    private final String displayDuration;
    private final double discountPercentage;

    SubscriptionPlan(int price, int durationMonths, String displayDuration, double discountPercentage) {
        this.price = price;
        this.durationMonths = durationMonths;
        this.displayDuration = displayDuration;
        this.discountPercentage = discountPercentage;
    }

    public int getPrice() {
        return price;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public String getDisplayDuration() {
        return displayDuration;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    // Calculate Bronze equivalent price
    public int getBronzeEquivalentPrice() {
        return 299 * durationMonths;
    }

    // Calculate savings
    public int getSavings() {
        return getBronzeEquivalentPrice() - price;
    }
}