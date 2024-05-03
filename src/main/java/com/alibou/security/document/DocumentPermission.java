package com.alibou.security.document;

import lombok.Data;

import com.alibou.security.user.User;

import jakarta.persistence.*;

@Data
@Entity

// @Idclass(CompositeKey.class)
// @Embeddable
public class DocumentPermission {
    @EmbeddedId
    private CompositeKey id;

    // @ManyToOne
    // @JoinColumn(referencedColumnName = "id", insertable = false, updatable =
    // false)
    private Long user;

    // @ManyToOne
    // @JoinColumn(referencedColumnName = "id", insertable = false, updatable =
    // false)
    private long document;

    @Enumerated(EnumType.STRING)
    private PermissionType permissionType;

    // public DocumentPermission(User user, Document document, PermissionType
    // permissionType) {
    // this.user = user;
    // // this.document = document;
    // this.permissionType = permissionType;
    // // this.id = new CompositeKey(user.getId(), document.getId());
    // }

    public DocumentPermission() {
    }

}
