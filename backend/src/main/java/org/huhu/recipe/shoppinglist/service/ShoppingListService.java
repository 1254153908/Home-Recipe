package org.huhu.recipe.shoppinglist.service;

import java.util.List;
import org.huhu.recipe.shoppinglist.dto.ShoppingListRequest;
import org.huhu.recipe.shoppinglist.dto.ShoppingListVO;

public interface ShoppingListService {

    ShoppingListVO generate(ShoppingListRequest request);

    void toggleItem(Long listId, Long itemId, boolean purchased);

    List<ShoppingListVO> getHistory();

    ShoppingListVO getDetail(Long id);

    void deleteList(Long id);
}
