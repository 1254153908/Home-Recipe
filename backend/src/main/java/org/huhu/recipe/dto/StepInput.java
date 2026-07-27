package org.huhu.recipe.dto;

import lombok.Data;

/** 单个步骤的输入（创建/AI 识别时提交，顺序即列表下标） */
@Data
public class StepInput {
    private String content;
    private String imageUrl;
}
