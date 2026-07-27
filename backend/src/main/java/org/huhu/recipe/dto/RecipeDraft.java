package org.huhu.recipe.dto;

import java.util.List;
import lombok.Data;

/** AI 识别返回的草稿，用于前端自动填充菜谱表单 */
@Data
public class RecipeDraft {
    private String title;
    private String imageUrl;
    private List<StepInput> steps;
    private List<ItemInput> ingredients;
    private List<ItemInput> seasonings;
}
