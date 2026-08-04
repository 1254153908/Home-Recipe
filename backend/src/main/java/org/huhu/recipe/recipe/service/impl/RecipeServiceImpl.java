package org.huhu.recipe.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.huhu.recipe.auth.config.UserContext;
import org.huhu.recipe.common.dto.ItemInput;
import org.huhu.recipe.common.dto.ItemView;
import org.huhu.recipe.common.dto.StepInput;
import org.huhu.recipe.common.service.FileUploadService;
import org.huhu.recipe.ingredient.entity.Ingredient;
import org.huhu.recipe.ingredient.service.IngredientService;
import org.huhu.recipe.recipe.dto.RecipeCreateRequest;
import org.huhu.recipe.recipe.dto.RecipeDetail;
import org.huhu.recipe.recipe.entity.Recipe;
import org.huhu.recipe.recipe.entity.RecipeFavorite;
import org.huhu.recipe.recipe.entity.RecipeIngredient;
import org.huhu.recipe.recipe.entity.RecipeSeasoning;
import org.huhu.recipe.recipe.entity.RecipeStep;
import org.huhu.recipe.recipe.mapper.RecipeFavoriteMapper;
import org.huhu.recipe.recipe.mapper.RecipeIngredientMapper;
import org.huhu.recipe.recipe.mapper.RecipeMapper;
import org.huhu.recipe.recipe.mapper.RecipeSeasoningMapper;
import org.huhu.recipe.recipe.mapper.RecipeStepMapper;
import org.huhu.recipe.recipe.service.RecipeService;
import org.huhu.recipe.seasoning.entity.Seasoning;
import org.huhu.recipe.seasoning.service.SeasoningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RecipeServiceImpl implements RecipeService {

    @Autowired
    private RecipeMapper recipeMapper;
    @Autowired
    private RecipeIngredientMapper recipeIngredientMapper;
    @Autowired
    private RecipeSeasoningMapper recipeSeasoningMapper;
    @Autowired
    private RecipeStepMapper recipeStepMapper;
    @Autowired
    private RecipeFavoriteMapper recipeFavoriteMapper;
    @Autowired
    private IngredientService ingredientService;
    @Autowired
    private SeasoningService seasoningService;
    @Autowired
    private FileUploadService fileUploadService;

    private String resolveImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        // 已经是内部存储的 URL，直接使用
        if (url.startsWith("/uploads/") || url.startsWith("/images/")) {
            return url;
        }
        // 外部 URL：下载后上传到自己的存储，失败时保留原始 URL 兜底
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                return fileUploadService.uploadFromUrl(url);
            } catch (Exception e) {
                return url;
            }
        }
        return url;
    }

    @Override
    public RecipeDetail create(RecipeCreateRequest request) {
        Recipe recipe = new Recipe();
        recipe.setTitle(request.getTitle());
        recipe.setImageUrl(resolveImageUrl(request.getImageUrl()));
        recipe.setSourceType(StringUtils.hasText(request.getSourceType()) ? request.getSourceType() : "manual");
        recipe.setSourceUrl(request.getSourceUrl());
        recipeMapper.insert(recipe);
        Long id = recipe.getId();
        saveItems(id, request.getIngredients(), request.getSeasonings());
        saveSteps(id, request.getSteps());
        return getDetail(id);
    }

    @Override
    public RecipeDetail getDetail(Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            return null;
        }
        RecipeDetail detail = new RecipeDetail();
        detail.setRecipe(recipe);
        detail.setSteps(loadSteps(id));
        detail.setIngredients(buildItemViews(id, true));
        detail.setSeasonings(buildItemViews(id, false));
        return detail;
    }

    @Override
    public List<Recipe> list() {
        return recipeMapper.selectList(null);
    }

    @Override
    public RecipeDetail update(Long id, RecipeCreateRequest request) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            return null;
        }

        // 封面图换了才删旧图（先 resolve 再比较，因为 request 可能带外部 URL）
        String resolvedCover = resolveImageUrl(request.getImageUrl());
        if (StringUtils.hasText(recipe.getImageUrl())
                && !recipe.getImageUrl().equals(resolvedCover)) {
            fileUploadService.delete(recipe.getImageUrl());
        }

        // 步骤图：收集新图的URL（先 resolve 统一为内部 URL，再和旧 URL 比较）
        Set<String> newStepUrls = new HashSet<>();
        if (request.getSteps() != null) {
            for (StepInput step : request.getSteps()) {
                if (StringUtils.hasText(step.getImageUrl())) {
                    newStepUrls.add(resolveImageUrl(step.getImageUrl()));
                }
            }
        }
        List<RecipeStep> oldSteps = loadSteps(id);
        for (RecipeStep oldStep : oldSteps) {
            if (StringUtils.hasText(oldStep.getImageUrl())
                    && !newStepUrls.contains(oldStep.getImageUrl())) {
                fileUploadService.delete(oldStep.getImageUrl());
            }
        }

        recipe.setTitle(request.getTitle());
        recipe.setImageUrl(resolvedCover);
        recipe.setSourceType(request.getSourceType());
        recipe.setSourceUrl(request.getSourceUrl());
        recipeMapper.updateById(recipe);
        deleteItems(id);
        deleteSteps(id);
        saveItems(id, request.getIngredients(), request.getSeasonings());
        saveSteps(id, request.getSteps());
        return getDetail(id);
    }

    @Override
    public void delete(Long id) {
        // 删除前先清理图片（MinIO/本地）
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe != null && StringUtils.hasText(recipe.getImageUrl())) {
            fileUploadService.delete(recipe.getImageUrl());
        }
        List<RecipeStep> steps = loadSteps(id);
        for (RecipeStep step : steps) {
            if (StringUtils.hasText(step.getImageUrl())) {
                fileUploadService.delete(step.getImageUrl());
            }
        }

        recipeMapper.deleteById(id);
        deleteItems(id);
        deleteSteps(id);
        QueryWrapper<RecipeFavorite> w = new QueryWrapper<>();
        w.eq("recipe_id", id);
        recipeFavoriteMapper.delete(w);
    }

    @Override
    public void favorite(Long recipeId) {
        Long userId = UserContext.getUserId();
        QueryWrapper<RecipeFavorite> w = new QueryWrapper<>();
        w.eq("recipe_id", recipeId).eq("user_id", userId);
        if (recipeFavoriteMapper.selectCount(w) > 0) {
            return;
        }
        RecipeFavorite favorite = new RecipeFavorite();
        favorite.setRecipeId(recipeId);
        favorite.setUserId(userId);
        recipeFavoriteMapper.insert(favorite);
    }

    @Override
    public void unfavorite(Long recipeId) {
        Long userId = UserContext.getUserId();
        QueryWrapper<RecipeFavorite> w = new QueryWrapper<>();
        w.eq("recipe_id", recipeId).eq("user_id", userId);
        recipeFavoriteMapper.delete(w);
    }

    @Override
    public List<Recipe> listFavorites() {
        Long userId = UserContext.getUserId();
        QueryWrapper<RecipeFavorite> w = new QueryWrapper<>();
        w.eq("user_id", userId);
        List<RecipeFavorite> favs = recipeFavoriteMapper.selectList(w);
        List<Long> ids = favs.stream().map(RecipeFavorite::getRecipeId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<Recipe> rw = new QueryWrapper<>();
        rw.in("id", ids);
        return recipeMapper.selectList(rw);
    }

    private void saveItems(Long recipeId, List<ItemInput> ingredients, List<ItemInput> seasonings) {
        if (ingredients != null) {
            for (ItemInput item : ingredients) {
                if (item == null || !StringUtils.hasText(item.getName())) {
                    continue;
                }
                Long ingId = ingredientService.resolveIdByName(item.getName());
                RecipeIngredient ri = new RecipeIngredient();
                ri.setRecipeId(recipeId);
                ri.setIngredientId(ingId);
                ri.setQuantity(item.getQuantity());
                ri.setUnit(item.getUnit());
                recipeIngredientMapper.insert(ri);
            }
        }
        if (seasonings != null) {
            for (ItemInput item : seasonings) {
                if (item == null || !StringUtils.hasText(item.getName())) {
                    continue;
                }
                Long seaId = seasoningService.resolveIdByName(item.getName());
                RecipeSeasoning rs = new RecipeSeasoning();
                rs.setRecipeId(recipeId);
                rs.setSeasoningId(seaId);
                rs.setQuantity(item.getQuantity());
                rs.setUnit(item.getUnit());
                recipeSeasoningMapper.insert(rs);
            }
        }
    }

    private void saveSteps(Long recipeId, List<StepInput> steps) {
        if (steps == null) {
            return;
        }
        int no = 1;
        for (StepInput step : steps) {
            if (step == null) {
                continue;
            }
            RecipeStep entity = new RecipeStep();
            entity.setRecipeId(recipeId);
            entity.setStepNo(no++);
            entity.setContent(step.getContent());
            entity.setImageUrl(resolveImageUrl(step.getImageUrl()));
            recipeStepMapper.insert(entity);
        }
    }

    private List<RecipeStep> loadSteps(Long recipeId) {
        QueryWrapper<RecipeStep> w = new QueryWrapper<>();
        w.eq("recipe_id", recipeId).orderByAsc("step_no");
        return recipeStepMapper.selectList(w);
    }

    private void deleteItems(Long recipeId) {
        QueryWrapper<RecipeIngredient> iw = new QueryWrapper<>();
        iw.eq("recipe_id", recipeId);
        recipeIngredientMapper.delete(iw);
        QueryWrapper<RecipeSeasoning> sw = new QueryWrapper<>();
        sw.eq("recipe_id", recipeId);
        recipeSeasoningMapper.delete(sw);
    }

    private void deleteSteps(Long recipeId) {
        QueryWrapper<RecipeStep> w = new QueryWrapper<>();
        w.eq("recipe_id", recipeId);
        recipeStepMapper.delete(w);
    }

    private List<ItemView> buildItemViews(Long recipeId, boolean ingredient) {
        List<ItemView> views = new ArrayList<>();
        if (ingredient) {
            QueryWrapper<RecipeIngredient> w = new QueryWrapper<>();
            w.eq("recipe_id", recipeId);
            for (RecipeIngredient ri : recipeIngredientMapper.selectList(w)) {
                ItemView view = new ItemView();
                view.setName(nameOf(ingredientService.list(), ri.getIngredientId()));
                view.setQuantity(ri.getQuantity());
                view.setUnit(ri.getUnit());
                views.add(view);
            }
        } else {
            QueryWrapper<RecipeSeasoning> w = new QueryWrapper<>();
            w.eq("recipe_id", recipeId);
            for (RecipeSeasoning rs : recipeSeasoningMapper.selectList(w)) {
                ItemView view = new ItemView();
                view.setName(nameOf(seasoningService.list(), rs.getSeasoningId()));
                view.setQuantity(rs.getQuantity());
                view.setUnit(rs.getUnit());
                views.add(view);
            }
        }
        return views;
    }

    private String nameOf(List<?> list, Long id) {
        if (list instanceof List && !list.isEmpty()) {
            if (list.get(0) instanceof Ingredient) {
                return ((List<Ingredient>) list).stream()
                        .filter(i -> i.getId().equals(id))
                        .map(Ingredient::getName).findFirst().orElse("");
            }
            if (list.get(0) instanceof Seasoning) {
                return ((List<Seasoning>) list).stream()
                        .filter(s -> s.getId().equals(id))
                        .map(Seasoning::getName).findFirst().orElse("");
            }
        }
        return "";
    }
}
