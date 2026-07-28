package org.huhu.recipe.ingredient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.huhu.recipe.ingredient.entity.Ingredient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IngredientMapper extends BaseMapper<Ingredient> {
}
