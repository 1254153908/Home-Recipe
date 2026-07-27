package org.huhu.recipe.dto;

import lombok.Data;

@Data
public class AiRecognizeRequest {
    /** link / video / image */
    private String sourceType;
    /** 链接地址，或图片/视频的本地路径/Base64（本 demo 仅作透传） */
    private String content;
}
