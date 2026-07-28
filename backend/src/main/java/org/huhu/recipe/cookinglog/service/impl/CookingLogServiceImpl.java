package org.huhu.recipe.cookinglog.service.impl;

import java.util.List;
import org.huhu.recipe.cookinglog.entity.CookingLog;
import org.huhu.recipe.cookinglog.mapper.CookingLogMapper;
import org.huhu.recipe.cookinglog.service.CookingLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CookingLogServiceImpl implements CookingLogService {

    @Autowired
    private CookingLogMapper cookingLogMapper;

    @Override
    public CookingLog create(CookingLog cookingLog) {
        cookingLogMapper.insert(cookingLog);
        return cookingLog;
    }

    @Override
    public CookingLog get(Long id) {
        return cookingLogMapper.selectById(id);
    }

    @Override
    public List<CookingLog> list() {
        return cookingLogMapper.selectList(null);
    }

    @Override
    public CookingLog update(Long id, CookingLog cookingLog) {
        cookingLog.setId(id);
        cookingLogMapper.updateById(cookingLog);
        return cookingLogMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        cookingLogMapper.deleteById(id);
    }
}
