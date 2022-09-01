package org.sterl.store.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockItemRepository extends JpaRepository<WarehouseItem, Long> {

    @Query("SELECT count(e) FROM WarehouseItem e WHERE e.item.id = :itemId")
    long countItems(long itemId);

}
