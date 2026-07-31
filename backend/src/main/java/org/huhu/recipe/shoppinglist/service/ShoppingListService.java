package org.huhu.recipe.shoppinglist.service;

import java.util.List;
import org.huhu.recipe.shoppinglist.dto.ShoppingListRequest;
import org.huhu.recipe.shoppinglist.dto.ShoppingListVO;

public interface ShoppingListService {

    ShoppingListVO generate(ShoppingListRequest request, Long userId);

    void toggleItem(Long listId, Long itemId, boolean purchased);

    List<ShoppingListVO> getHistory(Long userId);

    ShoppingListVO getDetail(Long id);

    void deleteList(Long id);
}
