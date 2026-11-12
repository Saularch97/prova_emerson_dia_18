package crudmanager.domain.model;

import java.time.LocalDate;

public class Post {
    private int id;
    private String content;
    private LocalDate postDate;
    private User user;

    public Post() {
        this(0);
    }

    public Post(int id) {
        this.id = id;
        setContent("");
        setPostDate(LocalDate.now());
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDate getPostDate() {
        return postDate;
    }

    public void setPostDate(LocalDate postDate) {
        this.postDate = postDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
