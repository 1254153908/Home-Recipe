package org.huhu.recipe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("seasoning")
public class Seasoning {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private LocalDateTime createdAt;
}
