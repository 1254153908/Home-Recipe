package org.huhu.recipe.shoppinglist.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("shopping_list")
public class ShoppingList {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}
