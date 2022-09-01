package org.sterl.store.warehouse;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.sterl.store.items.entity.Item;

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
