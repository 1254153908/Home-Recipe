package org.huhu.recipe.mealplan.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.huhu.recipe.common.dto.ItemView;
import org.huhu.recipe.recipe.entity.RecipeStep;

/** 根据膳食计划查询的菜谱完整信息（计划 + 菜谱 + 步骤 + 原料 + 调料） */
@Data
public class PlanRecipeDetailVO {
    // 计划信息
    private Long planId;
    private String remark;
    private String status;
    private String review;
    private String planImageUrl;
    private LocalDate planDate;
    // 菜谱信息
    private Long recipeId;
    private String title;
    private String recipeImageUrl;
    // 明细（一对多，由 XML 嵌套查询填充）
    private List<RecipeStep> steps;
    private List<ItemView> ingredients;
    private List<ItemView> seasonings;
}
