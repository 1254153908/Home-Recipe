package org.huhu.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.huhu.recipe.entity.Ingredient;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface IngredientMapper extends BaseMapper<Ingredient> {
}
