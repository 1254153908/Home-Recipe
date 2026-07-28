package org.huhu.recipe.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.huhu.recipe.recipe.entity.RecipeStep;

@Mapper
public interface RecipeStepMapper extends BaseMapper<RecipeStep> {
}
