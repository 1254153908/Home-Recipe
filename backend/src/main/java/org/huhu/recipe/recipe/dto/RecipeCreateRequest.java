package org.huhu.recipe.recipe.dto;

import java.util.List;
import lombok.Data;
import org.huhu.recipe.common.dto.StepInput;
import org.huhu.recipe.common.dto.ItemInput;

@Data
public class RecipeCreateRequest {
    private String title;
    private String imageUrl;
    private String sourceType;
    private String sourceUrl;
    private List<StepInput> steps;
    private List<ItemInput> ingredients;
    private List<ItemInput> seasonings;
}
