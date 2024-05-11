
package com.alibou.security.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetUserRequest {

    // You need to pass only the id or the email
    private Long userId;
    private String userEmail;

}
