package org.huhu.recipe.Service;

import java.util.List;
import org.huhu.recipe.dto.RecipeCreateRequest;
import org.huhu.recipe.dto.RecipeDetail;
import org.huhu.recipe.entity.Recipe;

public interface RecipeService {

    RecipeDetail create(RecipeCreateRequest request);

    RecipeDetail getDetail(Long id);

    List<Recipe> list();

    RecipeDetail update(Long id, RecipeCreateRequest request);

    void delete(Long id);

    void favorite(Long recipeId, Long userId);

    void unfavorite(Long recipeId, Long userId);

    List<Recipe> listFavorites(Long userId);
}
