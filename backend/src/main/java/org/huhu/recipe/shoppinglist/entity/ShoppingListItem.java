package org.huhu.recipe.shoppinglist.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("shopping_list_item")
public class ShoppingListItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shoppingListId;
    private Long ingredientId;
    private String ingredientName;
    private String totalQuantity;
    private String unit;
    private Integer isPurchased;
    private LocalDateTime purchasedAt;
}
