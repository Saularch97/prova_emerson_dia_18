package crudmanager.service;

public final class Pagination {

    public static final int PAGE_SIZE = 10;

    private Pagination() {
    }

    public static int parse(String raw) {
        if (raw == null) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static int clamp(int page, long totalItems) {
        long totalPages = (totalItems + PAGE_SIZE - 1) / PAGE_SIZE;
        return (int) Math.max(1, Math.min(page, totalPages));
    }

    public static int offset(int page) {
        return (page - 1) * PAGE_SIZE;
    }
}
