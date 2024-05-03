package com.alibou.security.document;

import lombok.Builder;
import lombok.Data;

import lombok.NoArgsConstructor;

import com.alibou.security.user.User;

import jakarta.persistence.*;

@Data
@Entity

@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Manager the relationship between the document and the owner
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;


    @Column(name = "title")
    private String title;

    public Document(String title) {
        this.title = title;
    }

    public Document() {
    }

    // create a new document
    public Document(User owner, String title) {
        this.owner = owner;
        this.title = title;
    }

    @Override
    public String toString() {
        return "Document [id=" + id + ", title=" + title + "]";
    }
}
