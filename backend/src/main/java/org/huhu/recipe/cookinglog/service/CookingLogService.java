package org.huhu.recipe.cookinglog.service;

import java.util.List;
import org.huhu.recipe.cookinglog.entity.CookingLog;

public interface CookingLogService {

    CookingLog create(CookingLog cookingLog);

    CookingLog get(Long id);

    List<CookingLog> list();

    CookingLog update(Long id, CookingLog cookingLog);

    void delete(Long id);
}
