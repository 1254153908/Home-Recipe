package org.huhu.recipe.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.huhu.recipe.auth.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {

    User selectByEmail(@Param("email") String email);

    User selectByUsername(@Param("username") String username);
}
