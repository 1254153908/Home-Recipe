package org.huhu.recipe.Service;

import java.util.List;
import org.huhu.recipe.entity.CookingLog;

public interface CookingLogService {

    CookingLog create(CookingLog cookingLog);

    CookingLog get(Long id);

    List<CookingLog> list();

    CookingLog update(Long id, CookingLog cookingLog);

    void delete(Long id);
}
