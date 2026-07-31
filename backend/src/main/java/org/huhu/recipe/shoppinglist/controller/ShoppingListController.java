package org.huhu.recipe.shoppinglist.controller;

import java.util.List;
import org.huhu.recipe.shoppinglist.dto.ShoppingListRequest;
import org.huhu.recipe.shoppinglist.dto.ShoppingListVO;
import org.huhu.recipe.shoppinglist.dto.TogglePurchasedRequest;
import org.huhu.recipe.shoppinglist.service.ShoppingListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopping-lists")
public class ShoppingListController {

    @Autowired
    private ShoppingListService shoppingListService;

    private static final long DEFAULT_USER_ID = 0L;

    @PostMapping
    public ShoppingListVO generate(@RequestBody ShoppingListRequest request,
                                   @RequestParam(defaultValue = "0") Long userId) {
        return shoppingListService.generate(request, userId);
    }

    @PutMapping("/{listId}/items/{itemId}")
    public void toggleItem(@PathVariable Long listId,
                           @PathVariable Long itemId,
                           @RequestBody TogglePurchasedRequest request) {
        shoppingListService.toggleItem(listId, itemId, request.isPurchased());
    }

    @GetMapping
    public List<ShoppingListVO> getHistory(@RequestParam(defaultValue = "0") Long userId) {
        return shoppingListService.getHistory(userId);
    }

    @GetMapping("/{id}")
    public ShoppingListVO getDetail(@PathVariable Long id) {
        return shoppingListService.getDetail(id);
    }

    @DeleteMapping("/{id}")
    public void deleteList(@PathVariable Long id) {
        shoppingListService.deleteList(id);
    }
}
