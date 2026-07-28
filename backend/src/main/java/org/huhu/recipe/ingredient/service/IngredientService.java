package org.huhu.recipe.ingredient.service;

import java.util.List;
import org.huhu.recipe.ingredient.entity.Ingredient;

public interface IngredientService {

    List<Ingredient> list();

    /** 按名称解析食材 id，不存在则新建后返回 id */
    Long resolveIdByName(String name);
}
