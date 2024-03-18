package org.xdove.thridpart.fastgpt;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.charset.Charset;

@Data
@AllArgsConstructor
public class Config {

    private final String apiUrl;
    private final String key;
    private final String chatKey;
    private final String charset;
    private final String version;


    public Config(String key, String chatKey) {
        this.apiUrl = "https://api.fastgpt.in";
        this.key = key;
        this.charset = Charset.defaultCharset().displayName();
        this.version = "v1";
        this.chatKey = chatKey;
    }

}