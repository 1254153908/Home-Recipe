package org.huhu.recipe.shoppinglist.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.huhu.recipe.auth.config.UserContext;
import org.huhu.recipe.shoppinglist.dto.ShoppingListItemVO;
import org.huhu.recipe.shoppinglist.dto.ShoppingListRequest;
import org.huhu.recipe.shoppinglist.dto.ShoppingListVO;
import org.huhu.recipe.shoppinglist.entity.ShoppingList;
import org.huhu.recipe.shoppinglist.entity.ShoppingListItem;
import org.huhu.recipe.shoppinglist.mapper.ShoppingListItemMapper;
import org.huhu.recipe.shoppinglist.mapper.ShoppingListMapper;
import org.huhu.recipe.shoppinglist.service.ShoppingListService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingListServiceImpl implements ShoppingListService {

    @Autowired
    private ShoppingListMapper shoppingListMapper;

    @Autowired
    private ShoppingListItemMapper shoppingListItemMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final long CACHE_TTL_SECONDS = 3600;
    private static final String CACHE_KEY_PREFIX = "shopping";
    private static final String LOCK_KEY_PREFIX = "shopping:lock";

    @Override
    @Transactional
    public ShoppingListVO generate(ShoppingListRequest request) {
        Long userId = UserContext.getUserId();
        String lockKey = LOCK_KEY_PREFIX + ":" + userId + ":" + request.getStartDate() + ":" + request.getEndDate();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock(10, TimeUnit.SECONDS);
            return doGenerate(request, userId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private ShoppingListVO doGenerate(ShoppingListRequest request, Long userId) {
        // 覆盖式：同日期范围的旧清单删除
        List<ShoppingList> existing = shoppingListMapper.selectList(
                new LambdaQueryWrapper<ShoppingList>()
                        .eq(ShoppingList::getUserId, userId)
                        .eq(ShoppingList::getStartDate, request.getStartDate())
                        .eq(ShoppingList::getEndDate, request.getEndDate()));
        for (ShoppingList old : existing) {
            shoppingListItemMapper.delete(
                    new LambdaQueryWrapper<ShoppingListItem>()
                            .eq(ShoppingListItem::getShoppingListId, old.getId()));
            shoppingListMapper.deleteById(old.getId());
        }
        redisTemplate.delete(buildCacheKey(userId, request));
        redisTemplate.delete(CACHE_KEY_PREFIX + ":history:" + userId);

        List<Map<String, Object>> rows = shoppingListMapper.aggregateIngredients(
                request.getStartDate().toString(), request.getEndDate().toString());

        ShoppingList sl = new ShoppingList();
        sl.setUserId(userId);
        sl.setStartDate(request.getStartDate());
        sl.setEndDate(request.getEndDate());
        shoppingListMapper.insert(sl);

        List<ShoppingListItemVO> itemVOs = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ShoppingListItem item = new ShoppingListItem();
            item.setShoppingListId(sl.getId());
            Object ingId = row.get("ingredient_id");
            item.setIngredientId(ingId instanceof Number ? ((Number) ingId).longValue() : Long.valueOf(ingId.toString()));
            item.setIngredientName((String) row.get("ingredient_name"));
            item.setUnit((String) row.get("unit"));
            item.setTotalQuantity(formatQuantity(row.get("total_quantity")));
            item.setIsPurchased(0);
            shoppingListItemMapper.insert(item);

            ShoppingListItemVO vo = new ShoppingListItemVO();
            vo.setId(item.getId());
            vo.setIngredientId(item.getIngredientId());
            vo.setName(item.getIngredientName());
            vo.setQuantity(item.getTotalQuantity());
            vo.setUnit(item.getUnit());
            vo.setIsPurchased(false);
            itemVOs.add(vo);
        }

        ShoppingListVO result = new ShoppingListVO();
        result.setId(sl.getId());
        result.setUserId(userId);
        result.setStartDate(sl.getStartDate());
        result.setEndDate(sl.getEndDate());
        result.setCreatedAt(sl.getCreatedAt());
        result.setItems(itemVOs);
        return result;
    }

    @Override
    public void toggleItem(Long listId, Long itemId, boolean purchased) {
        ShoppingListItem item = shoppingListItemMapper.selectById(itemId);
        if (item == null || !item.getShoppingListId().equals(listId)) {
            return;
        }
        item.setIsPurchased(purchased ? 1 : 0);
        item.setPurchasedAt(purchased ? LocalDateTime.now() : null);
        shoppingListItemMapper.updateById(item);

        ShoppingList sl = shoppingListMapper.selectById(listId);
        if (sl != null) {
            ShoppingListRequest req = new ShoppingListRequest();
            req.setStartDate(sl.getStartDate());
            req.setEndDate(sl.getEndDate());
            redisTemplate.delete(buildCacheKey(sl.getUserId(), req));
            redisTemplate.delete(CACHE_KEY_PREFIX + ":history:" + sl.getUserId());
        }
    }

    @Override
    public List<ShoppingListVO> getHistory() {
        Long userId = UserContext.getUserId();
        String historyKey = CACHE_KEY_PREFIX + ":history:" + userId;
        @SuppressWarnings("unchecked")
        List<ShoppingListVO> cached = (List<ShoppingListVO>) redisTemplate.opsForValue().get(historyKey);
        if (cached != null) {
            return cached;
        }

        List<ShoppingList> lists = shoppingListMapper.selectList(
                new LambdaQueryWrapper<ShoppingList>()
                        .eq(ShoppingList::getUserId, userId)
                        .orderByDesc(ShoppingList::getCreatedAt));

        List<ShoppingListVO> result = lists.stream().map(sl -> {
            ShoppingListVO vo = new ShoppingListVO();
            vo.setId(sl.getId());
            vo.setUserId(sl.getUserId());
            vo.setStartDate(sl.getStartDate());
            vo.setEndDate(sl.getEndDate());
            vo.setCreatedAt(sl.getCreatedAt());
            List<ShoppingListItem> items = shoppingListItemMapper.selectList(
                    new LambdaQueryWrapper<ShoppingListItem>()
                            .eq(ShoppingListItem::getShoppingListId, sl.getId()));
            vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());

        redisTemplate.opsForValue().set(historyKey, result, Duration.ofSeconds(CACHE_TTL_SECONDS));
        return result;
    }

    @Override
    public ShoppingListVO getDetail(Long id) {
        ShoppingList sl = shoppingListMapper.selectById(id);
        if (sl == null) {
            return null;
        }
        ShoppingListVO vo = new ShoppingListVO();
        vo.setId(sl.getId());
        vo.setUserId(sl.getUserId());
        vo.setStartDate(sl.getStartDate());
        vo.setEndDate(sl.getEndDate());
        vo.setCreatedAt(sl.getCreatedAt());
        List<ShoppingListItem> items = shoppingListItemMapper.selectList(
                new LambdaQueryWrapper<ShoppingListItem>()
                        .eq(ShoppingListItem::getShoppingListId, id));
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional
    public void deleteList(Long id) {
        ShoppingList sl = shoppingListMapper.selectById(id);
        if (sl == null) {
            return;
        }
        shoppingListItemMapper.delete(
                new LambdaQueryWrapper<ShoppingListItem>()
                        .eq(ShoppingListItem::getShoppingListId, id));
        shoppingListMapper.deleteById(id);

        ShoppingListRequest req = new ShoppingListRequest();
        req.setStartDate(sl.getStartDate());
        req.setEndDate(sl.getEndDate());
        redisTemplate.delete(buildCacheKey(sl.getUserId(), req));
        redisTemplate.delete(CACHE_KEY_PREFIX + ":history:" + sl.getUserId());
    }

    private ShoppingListItemVO toItemVO(ShoppingListItem item) {
        ShoppingListItemVO vo = new ShoppingListItemVO();
        vo.setId(item.getId());
        vo.setIngredientId(item.getIngredientId());
        vo.setName(item.getIngredientName());
        vo.setQuantity(item.getTotalQuantity());
        vo.setUnit(item.getUnit());
        vo.setIsPurchased(item.getIsPurchased() != null && item.getIsPurchased() == 1);
        vo.setPurchasedAt(item.getPurchasedAt());
        return vo;
    }

    private String formatQuantity(Object val) {
        if (val == null) return null;
        if (val instanceof Number) {
            BigDecimal bd = new BigDecimal(val.toString());
            if (bd.scale() <= 0) return bd.toBigInteger().toString();
            return bd.stripTrailingZeros().toPlainString();
        }
        return val.toString();
    }

    private String buildCacheKey(Long userId, ShoppingListRequest req) {
        return CACHE_KEY_PREFIX + ":" + userId + ":" + req.getStartDate() + ":" + req.getEndDate();
    }

}
