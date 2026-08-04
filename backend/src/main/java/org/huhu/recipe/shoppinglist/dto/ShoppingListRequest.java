package org.huhu.recipe.shoppinglist.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ShoppingListRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}
