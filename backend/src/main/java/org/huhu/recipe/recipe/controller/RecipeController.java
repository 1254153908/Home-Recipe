package org.huhu.recipe.recipe.controller;

import java.util.List;
import org.huhu.recipe.auth.config.UserContext;
import org.huhu.recipe.common.dto.AiRecognizeRequest;
import org.huhu.recipe.common.dto.RecipeDraft;
import org.huhu.recipe.recipe.dto.RecipeCreateRequest;
import org.huhu.recipe.recipe.dto.RecipeDetail;
import org.huhu.recipe.recipe.entity.Recipe;
import org.huhu.recipe.recipe.service.AiRecognitionService;
import org.huhu.recipe.recipe.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;
    @Autowired
    private AiRecognitionService aiRecognitionService;

    @PostMapping
    public RecipeDetail create(@RequestBody RecipeCreateRequest request) {
        return recipeService.create(request);
    }

    @PutMapping("/{id}")
    public RecipeDetail update(@PathVariable Long id, @RequestBody RecipeCreateRequest request) {
        return recipeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        recipeService.delete(id);
    }

    @GetMapping("/{id}")
    public RecipeDetail get(@PathVariable Long id) {
        return recipeService.getDetail(id);
    }

    @GetMapping
    public List<Recipe> list() {
        return recipeService.list();
    }

    @PostMapping("/{id}/favorite")
    public void favorite(@PathVariable Long id) {
        recipeService.favorite(id);
    }

    @DeleteMapping("/{id}/favorite")
    public void unfavorite(@PathVariable Long id) {
        recipeService.unfavorite(id);
    }

    @GetMapping("/favorites")
    public List<Recipe> favorites() {
        return recipeService.listFavorites();
    }

    /** AI 识别链接/视频/图片，返回可自动填充的菜谱草稿 */
    @PostMapping("/ai-recognize")
    public RecipeDraft aiRecognize(@RequestBody AiRecognizeRequest request) {
        return aiRecognitionService.recognize(request);
    }
}
