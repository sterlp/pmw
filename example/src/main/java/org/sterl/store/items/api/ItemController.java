package org.sterl.store.items.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sterl.store.items.ItemService;
import org.sterl.store.items.entity.Item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
@Validated
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{id}")
    ResponseEntity<Item> create(@PathVariable long id) {
        return ResponseEntity.of(itemService.get(id));
    }
    @PostMapping
    Item create(@RequestBody @Valid Item item) {
        return itemService.createNewItem(item);
    }
}
