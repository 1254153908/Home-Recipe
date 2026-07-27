package org.huhu.recipe.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.huhu.recipe.Service.IngredientService;
import org.huhu.recipe.entity.Ingredient;
import org.huhu.recipe.mapper.IngredientMapper;
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
