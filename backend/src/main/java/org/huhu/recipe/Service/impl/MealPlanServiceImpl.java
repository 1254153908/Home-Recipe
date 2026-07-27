package org.huhu.recipe.Service.impl;

import java.util.List;
import org.huhu.recipe.Service.MealPlanService;
import org.huhu.recipe.dto.PlanRecipeDetailVO;
import org.huhu.recipe.entity.MealPlan;
import org.huhu.recipe.mapper.MealPlanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MealPlanServiceImpl implements MealPlanService {

    @Autowired
    private MealPlanMapper mealPlanMapper;

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
        return mealPlanMapper.selectById(id);
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
