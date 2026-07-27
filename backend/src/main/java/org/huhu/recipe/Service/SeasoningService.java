package org.huhu.recipe.Service;

import java.util.List;
import org.huhu.recipe.entity.Seasoning;

public interface SeasoningService {

    List<Seasoning> list();

    /** 按名称解析调料 id，不存在则新建后返回 id */
    Long resolveIdByName(String name);
}
