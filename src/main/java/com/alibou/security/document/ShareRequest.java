package com.alibou.security.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShareRequest {
  // private Long userId;
  private String userEmail;
  // private Long documentId;
  private String permissionType;
}
