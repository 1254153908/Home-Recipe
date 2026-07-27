package org.huhu.recipe.dto;

import java.util.List;
import lombok.Data;
import org.huhu.recipe.entity.Recipe;
import org.huhu.recipe.entity.RecipeStep;

@Data
public class RecipeDetail {
    private Recipe recipe;
    private List<RecipeStep> steps;
    private List<ItemView> ingredients;
    private List<ItemView> seasonings;
}
