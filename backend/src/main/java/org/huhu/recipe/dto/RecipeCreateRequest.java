package org.huhu.recipe.dto;

import java.util.List;
import lombok.Data;

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
