package com.alibou.security.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RenameRequest {
    private String title;

    public String getTitle() {
        return title;
    }
}
