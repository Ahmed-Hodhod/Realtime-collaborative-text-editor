package com.alibou.security.document;

import java.io.Serializable;

import com.alibou.security.user.User;

@jakarta.persistence.Embeddable

public class CompositeKey implements Serializable {
    private Long documentId;
    private Long userId;

    // define the constructor
    public CompositeKey(Long doc, Long user) {
        this.documentId = doc;
        this.userId = user;
    }

    public CompositeKey() {
    }
}