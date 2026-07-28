package org.huhu.recipe.recipe.dto;

import java.util.List;
import lombok.Data;
import org.huhu.recipe.recipe.entity.Recipe;
import org.huhu.recipe.recipe.entity.RecipeStep;
import org.huhu.recipe.common.dto.ItemView;

@Data
public class RecipeDetail {
    private Recipe recipe;
    private List<RecipeStep> steps;
    private List<ItemView> ingredients;
    private List<ItemView> seasonings;
}
