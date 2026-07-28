package org.huhu.recipe.Service.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.huhu.recipe.Service.MealPlanService;
import org.huhu.recipe.dto.PlanRecipeDetailVO;
import org.huhu.recipe.entity.CookingLog;
import org.huhu.recipe.entity.MealPlan;
import org.huhu.recipe.entity.Recipe;
import org.huhu.recipe.mapper.CookingLogMapper;
import org.huhu.recipe.mapper.MealPlanMapper;
import org.huhu.recipe.mapper.RecipeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MealPlanServiceImpl implements MealPlanService {

    @Autowired
    private MealPlanMapper mealPlanMapper;

    @Autowired
    private CookingLogMapper cookingLogMapper;

    @Autowired
    private RecipeMapper recipeMapper;

    @Override
    public MealPlan create(MealPlan mealPlan) {
        mealPlanMapper.insert(mealPlan);
        return mealPlan;
    }

    @Override
    public MealPlan get(Long id) {
        return mealPlanMapper.selectById(id);
    }

    @Override
    public List<MealPlan> list() {
        return mealPlanMapper.selectList(null);
    }

    @Override
    public MealPlan update(Long id, MealPlan mealPlan) {
        mealPlan.setId(id);
        mealPlanMapper.updateById(mealPlan);

        MealPlan updated = mealPlanMapper.selectById(id);

        // 当状态变为 done 时，自动创建烹饪日志
        if ("done".equals(updated.getStatus())) {
            CookingLog existing = cookingLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CookingLog>()
                    .eq(CookingLog::getPlanId, id)
            ).stream().findFirst().orElse(null);

            if (existing == null) {
                CookingLog log = new CookingLog();
                log.setPlanId(id);
                log.setRecipeId(updated.getRecipeId());
                log.setPlanDate(updated.getPlanDate());
                log.setCompletedAt(LocalDateTime.now());

                // 查菜谱标题
                if (updated.getRecipeId() != null) {
                    Recipe recipe = recipeMapper.selectById(updated.getRecipeId());
                    if (recipe != null) {
                        log.setRecipeTitle(recipe.getTitle());
                    }
                }

                cookingLogMapper.insert(log);
            }
        }

        return updated;
    }

    @Override
    public void delete(Long id) {
        mealPlanMapper.deleteById(id);
    }

    @Override
    public PlanRecipeDetailVO getPlanRecipeDetail(Long planId) {
        return mealPlanMapper.selectPlanRecipeDetail(planId);
    }
}
