package org.huhu.recipe.recipe.service;

import java.util.List;
import org.huhu.recipe.recipe.dto.RecipeCreateRequest;
import org.huhu.recipe.recipe.dto.RecipeDetail;
import org.huhu.recipe.recipe.entity.Recipe;

public interface RecipeService {

    RecipeDetail create(RecipeCreateRequest request);

    RecipeDetail getDetail(Long id);

    List<Recipe> list();

    RecipeDetail update(Long id, RecipeCreateRequest request);

    void delete(Long id);

    void favorite(Long recipeId);

    void unfavorite(Long recipeId);

    List<Recipe> listFavorites();
}
