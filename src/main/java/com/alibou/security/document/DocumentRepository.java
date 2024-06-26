package com.alibou.security.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.alibou.security.user.User;

public interface DocumentRepository extends CrudRepository<Document, Long> {
    List<Document> findByTitleContaining(String title);

    // Document deleteById();

}