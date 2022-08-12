package org.sterl.pmw.testapp.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sterl.pmw.testapp.item.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Item findByName(String name);
}
