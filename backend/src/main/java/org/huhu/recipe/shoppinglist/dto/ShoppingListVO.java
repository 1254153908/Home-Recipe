package org.huhu.recipe.shoppinglist.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ShoppingListVO {
    private Long id;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private List<ShoppingListItemVO> items;
}
