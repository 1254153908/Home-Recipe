package org.huhu.recipe.mealplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.huhu.recipe.mealplan.dto.PlanRecipeDetailVO;
import org.huhu.recipe.mealplan.entity.MealPlan;

@Mapper
public interface MealPlanMapper extends BaseMapper<MealPlan> {

    /** 根据计划 id 联查菜谱内容、原料、调料（复杂查询，SQL 见 MealPlanMapper.xml） */
    PlanRecipeDetailVO selectPlanRecipeDetail(@Param("planId") Long planId);
}
