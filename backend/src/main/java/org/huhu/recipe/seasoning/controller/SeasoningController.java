package org.huhu.recipe.seasoning.controller;

import java.util.List;
import org.huhu.recipe.seasoning.entity.Seasoning;
import org.huhu.recipe.seasoning.service.SeasoningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seasonings")
public class SeasoningController {

    @Autowired
    private SeasoningService seasoningService;

    @GetMapping
    public List<Seasoning> list() {
        return seasoningService.list();
    }

    @PostMapping
    public Long create(@RequestParam String name) {
        return seasoningService.resolveIdByName(name);
    }
}
