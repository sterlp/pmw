package org.sterl.store.warehouse;

import org.sterl.store.items.entity.Item;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseItem {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false, cascade = {})
    @JoinColumn(name = "ITEM_ID", nullable = false, updatable = false)
    private Item item;

    @NotNull
    private String barcode;
}
