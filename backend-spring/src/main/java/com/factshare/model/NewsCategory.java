package com.factshare.model;

import java.util.List;

/**
 * Canonical news categories used to tag every verified claim and to filter
 * the Community Feed.
 */
public final class NewsCategory {
    public static final List<String> CATEGORIES = List.of(
        "Politics", "Business", "Technology", "Sports", "Entertainment",
        "Science", "Health", "World", "Local", "Crime", "Other");

    private NewsCategory() {}

    /** Case-insensitive match to a canonical category; falls back to "Other". */
    public static String normalize(String raw) {
        if (raw == null) return "Other";
        String value = raw.trim();
        if (value.isEmpty()) return "Other";
        for (String c : CATEGORIES) {
            if (c.equalsIgnoreCase(value)) return c;
        }
        return "Other";
    }
}
