package org.xdove.thridpart.fastgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ChatMessage {
    private String content;
    private String role;

    public ChatMessage(String content) {
        this.content = content;
        this.role = "user";
    }
}
