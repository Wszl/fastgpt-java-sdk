package org.xdove.thridpart.fastgpt.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class PushData {
    private String q;
    private String a;
    private List<DataIndex> indexes;

    @Data
    @AllArgsConstructor
    public static class DataIndex {
        private boolean defaultIndex;
        private String type;
        private String text;
    }
}
