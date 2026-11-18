package crudmanager.model;

import java.util.List;

public final class Page<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long totalItems;

    private Page(List<T> items, int page, int pageSize, long totalItems) {
        this.items = List.copyOf(items);
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
    }

    public static <T> Page<T> of(List<T> items, int page, int pageSize, long totalItems) {
        return new Page<>(items, page, pageSize, totalItems);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return (int) ((totalItems + pageSize - 1) / pageSize);
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < getTotalPages();
    }
}
