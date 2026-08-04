package org.huhu.recipe.shoppinglist.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ShoppingListItemVO {
    private Long id;
    private Long ingredientId;
    private String name;
    private String quantity;
    private String unit;
    private Boolean isPurchased;
    private LocalDateTime purchasedAt;
}
