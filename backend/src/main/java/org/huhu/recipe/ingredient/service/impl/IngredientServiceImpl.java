package org.huhu.recipe.ingredient.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.huhu.recipe.ingredient.entity.Ingredient;
import org.huhu.recipe.ingredient.mapper.IngredientMapper;
import org.huhu.recipe.ingredient.service.IngredientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IngredientServiceImpl implements IngredientService {

    @Autowired
    private IngredientMapper ingredientMapper;

    @Override
    public List<Ingredient> list() {
        return ingredientMapper.selectList(null);
    }

    @Override
    public Long resolveIdByName(String name) {
        QueryWrapper<Ingredient> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name);
        Ingredient existing = ingredientMapper.selectOne(wrapper);
        if (existing != null) {
            return existing.getId();
        }
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredientMapper.insert(ingredient);
        return ingredient.getId();
    }
}
