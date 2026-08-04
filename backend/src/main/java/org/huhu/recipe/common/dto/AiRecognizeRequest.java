package org.huhu.recipe.common.dto;

import lombok.Data;

@Data
public class AiRecognizeRequest {
    /** link / image */
    private String sourceType;
    /** URL 地址 或 base64 图片数据列表（多图时传多个 base64） */
    private java.util.List<String> content;
    /** 来源域名提示，用于选择解析策略（如下厨房直接 HTML 解析） */
    private String urlHint;
}
