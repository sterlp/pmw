package org.sterl.store.items.entity;

import java.math.BigDecimal;
import java.math.MathContext;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

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

    @NotNull
    private String name;

    @NotNull
    private BigDecimal price;

    @Builder.Default
    private long inStock = 0;

    public BigDecimal applyDiscount(BigDecimal percent) {
        var result = price;
        price = price.multiply(percent, new MathContext(2));
        return result;
    }
}
