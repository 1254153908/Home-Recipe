package org.huhu.recipe.Controller;

import java.util.List;
import org.huhu.recipe.Service.IngredientService;
import org.huhu.recipe.entity.Ingredient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    @Autowired
    private IngredientService ingredientService;

    @GetMapping
    public List<Ingredient> list() {
        return ingredientService.list();
    }

    @PostMapping
    public Long create(@RequestParam String name) {
        return ingredientService.resolveIdByName(name);
    }
}
