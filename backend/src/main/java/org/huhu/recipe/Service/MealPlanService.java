package org.huhu.recipe.Service;

import java.util.List;
import org.huhu.recipe.dto.PlanRecipeDetailVO;
import org.huhu.recipe.entity.MealPlan;

public interface MealPlanService {

    MealPlan create(MealPlan mealPlan);

    MealPlan get(Long id);

    List<MealPlan> list();

    MealPlan update(Long id, MealPlan mealPlan);

    void delete(Long id);

    /** 根据计划 id 查询其菜谱的完整内容（原料、调料等） */
    PlanRecipeDetailVO getPlanRecipeDetail(Long planId);
}
