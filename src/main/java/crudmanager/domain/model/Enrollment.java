package crudmanager.domain.model;

import java.time.LocalDate;

public class Enrollment {

    public enum Status {
        ATIVA("Ativa"),
        CONCLUIDA("Concluída"),
        CANCELADA("Cancelada");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private int id;
    private User student;
    private Course course;
    private LocalDate enrolledOn;
    private Status status;
    private int progress;
    private boolean paid;

    public Enrollment() {
        this(0);
    }

    public Enrollment(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public LocalDate getEnrolledOn() {
        return enrolledOn;
    }

    public void setEnrolledOn(LocalDate enrolledOn) {
        this.enrolledOn = enrolledOn;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }
}
