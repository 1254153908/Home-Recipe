package org.huhu.recipe.Controller;

import java.util.List;
import org.huhu.recipe.Service.MealPlanService;
import org.huhu.recipe.dto.PlanRecipeDetailVO;
import org.huhu.recipe.entity.MealPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-plans")
public class MealPlanController {

    @Autowired
    private MealPlanService mealPlanService;

    @PostMapping
    public MealPlan create(@RequestBody MealPlan mealPlan) {
        return mealPlanService.create(mealPlan);
    }

    @GetMapping
    public List<MealPlan> list() {
        return mealPlanService.list();
    }

    @GetMapping("/{id}")
    public MealPlan get(@PathVariable Long id) {
        return mealPlanService.get(id);
    }

    @PutMapping("/{id}")
    public MealPlan update(@PathVariable Long id, @RequestBody MealPlan mealPlan) {
        return mealPlanService.update(id, mealPlan);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        mealPlanService.delete(id);
    }

    /** 根据计划 id 查询其菜谱的完整内容（含原料、调料） */
    @GetMapping("/{id}/recipe")
    public PlanRecipeDetailVO recipeDetail(@PathVariable Long id) {
        return mealPlanService.getPlanRecipeDetail(id);
    }
}
