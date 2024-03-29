package org.sterl.pmw.testapp.item.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Item {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private BigDecimal price;

    @Builder.Default
    private int inStock = 0;
}
