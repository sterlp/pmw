package org.sterl.store.items.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.sterl.store.items.entity.Item;

@SpringBootTest
@ActiveProfiles("test")
class ItemServiceTest {

    @Autowired ItemService subject;

    @BeforeEach
    void setUp() throws Exception {
    }

    @Test
    void test() {
        // GIVEN
        var item = Item.builder().name("Foo1").price(new BigDecimal("12.99")).build();

        // WHEN
        subject.createNewItem(item);

        // THEN
        assertThat(subject.get(item.getId())).isPresent();
    }

}
