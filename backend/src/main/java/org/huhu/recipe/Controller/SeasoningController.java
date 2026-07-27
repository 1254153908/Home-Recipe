package org.huhu.recipe.Controller;

import java.util.List;
import org.huhu.recipe.Service.SeasoningService;
import org.huhu.recipe.entity.Seasoning;
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
