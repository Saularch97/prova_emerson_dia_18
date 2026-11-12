package crudmanager.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Course {

    public enum Level {
        INICIANTE("Iniciante"),
        INTERMEDIARIO("Intermediário"),
        AVANCADO("Avançado");

        private final String label;

        Level(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private int id;
    private String title;
    private String description;
    private BigDecimal price;
    private LocalDate startDate;
    private Level level;
    private boolean active;
    private User instructor;

    public Course() {
        this(0);
    }

    public Course(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public User getInstructor() {
        return instructor;
    }

    public void setInstructor(User instructor) {
        this.instructor = instructor;
    }
}
