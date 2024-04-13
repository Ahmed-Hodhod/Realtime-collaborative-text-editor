package com.alibou.security.document;


import jakarta.persistence.*;

@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "title")
    private String title;

    @Column(name = "ownerID")
    private long ownerID;
    public Document() {
    }
    public Document(String title, long ownerID) {
        this.title = title;
        this.ownerID = ownerID;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Long getOwnerID() {
        return ownerID;
    }


    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "Document [id=" + id + ", title=" + title + "]";
    }
}
