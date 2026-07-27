package org.huhu.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.huhu.recipe.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {
}
