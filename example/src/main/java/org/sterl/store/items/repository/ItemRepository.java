package org.sterl.store.items.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sterl.store.items.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

}
