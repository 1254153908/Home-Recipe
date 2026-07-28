package org.huhu.recipe.cookinglog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.huhu.recipe.cookinglog.entity.CookingLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CookingLogMapper extends BaseMapper<CookingLog> {
}
