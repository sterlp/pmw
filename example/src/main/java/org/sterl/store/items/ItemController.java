package org.sterl.store.items;

import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
@Validated
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    Item create(@RequestBody @Valid Item item) {
        return itemService.createNewItem(item);
    }
}
