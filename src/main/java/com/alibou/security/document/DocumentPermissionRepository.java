package com.alibou.security.document;

import java.util.List;

// import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.alibou.security.document.CompositeKey;

public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, CompositeKey> {

    List<DocumentPermission> findByUser(Long id);
    // List<Document> findByTitleContaining(String title);
    
}