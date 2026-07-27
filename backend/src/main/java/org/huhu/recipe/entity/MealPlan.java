package org.huhu.recipe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("meal_plan")
public class MealPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipeId;
    private String remark;
    private String status;
    private String review;
    private String imageUrl;
    private LocalDate planDate;
    private LocalDateTime createdAt;
}
