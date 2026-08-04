package org.huhu.recipe.shoppinglist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.huhu.recipe.shoppinglist.entity.ShoppingList;

public interface ShoppingListMapper extends BaseMapper<ShoppingList> {

    List<Map<String, Object>> aggregateIngredients(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
}
