package org.sterl.store.items.workflow;

import java.io.Serializable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.WorkflowUmlService;
import org.sterl.pmw.component.SerializationUtil;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.spring.persistent_tasks.api.TaskId;
import org.sterl.store.items.component.DiscountComponent;
import org.sterl.store.items.component.UpdateInStockCountComponent;
import org.sterl.store.items.component.WarehouseStockComponent;
import org.sterl.store.warehouse.WarehouseService;

@ExtendWith(MockitoExtension.class)
class NewItemArrivedWorkflowMockTest {

    @Mock WarehouseService warehouseService;
    @Mock DiscountComponent discountComponent;
    @Mock WarehouseStockComponent createStock;
    @Mock UpdateInStockCountComponent updateStock;
    @Mock WorkflowService<TaskId<? extends Serializable>> workflowService;

    @InjectMocks NewItemArrivedWorkflow subject;

    @BeforeEach
    void setUp() throws Exception {
        subject.createWorkflow();
    }

    @Test
    void test() throws Exception {
        WorkflowRepository repo = new WorkflowRepository();
        WorkflowUmlService umlService = new WorkflowUmlService(repo);

        repo.register(subject.getCheckWarehouse());
        repo.register(subject.getRestorePriceSubWorkflow());

        SerializationUtil.writeAsPlantUmlSvg("./check-warehouse.svg", subject.getCheckWarehouse().getName(), umlService);
    }
}
