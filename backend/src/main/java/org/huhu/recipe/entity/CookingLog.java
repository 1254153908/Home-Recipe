package org.huhu.recipe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("cooking_log")
public class CookingLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long recipeId;
    private String recipeTitle;
    private LocalDate planDate;
    private LocalDateTime completedAt;
    private String imageUrl;
    private String review;
    private LocalDateTime createdAt;
}
