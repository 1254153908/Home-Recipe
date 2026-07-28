package org.huhu.recipe.seasoning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.huhu.recipe.seasoning.entity.Seasoning;
import org.huhu.recipe.seasoning.mapper.SeasoningMapper;
import org.huhu.recipe.seasoning.service.SeasoningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeasoningServiceImpl implements SeasoningService {

    @Autowired
    private SeasoningMapper seasoningMapper;

    @Override
    public List<Seasoning> list() {
        return seasoningMapper.selectList(null);
    }

    @Override
    public Long resolveIdByName(String name) {
        QueryWrapper<Seasoning> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name);
        Seasoning existing = seasoningMapper.selectOne(wrapper);
        if (existing != null) {
            return existing.getId();
        }
        Seasoning seasoning = new Seasoning();
        seasoning.setName(name);
        seasoningMapper.insert(seasoning);
        return seasoning.getId();
    }
}
