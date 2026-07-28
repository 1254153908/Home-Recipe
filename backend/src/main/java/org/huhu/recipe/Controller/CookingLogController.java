package org.huhu.recipe.Controller;

import java.util.List;
import org.huhu.recipe.Service.CookingLogService;
import org.huhu.recipe.entity.CookingLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cooking-logs")
public class CookingLogController {

    @Autowired
    private CookingLogService cookingLogService;

    @PostMapping
    public CookingLog create(@RequestBody CookingLog cookingLog) {
        return cookingLogService.create(cookingLog);
    }

    @GetMapping
    public List<CookingLog> list() {
        return cookingLogService.list();
    }

    @GetMapping("/{id}")
    public CookingLog get(@PathVariable Long id) {
        return cookingLogService.get(id);
    }

    @PutMapping("/{id}")
    public CookingLog update(@PathVariable Long id, @RequestBody CookingLog cookingLog) {
        return cookingLogService.update(id, cookingLog);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        cookingLogService.delete(id);
    }
}
