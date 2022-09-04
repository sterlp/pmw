package org.sterl.store.items.workflow;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.boundary.WorkflowUmlService;
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
    @Mock WorkflowService<JobDetail> workflowService;

    @InjectMocks NewItemArrivedWorkflow subject;

    @BeforeEach
    void setUp() throws Exception {
        subject.createWorkflow();
    }

    @Test
    void test() throws Exception {
        File d = new File("./check-warehouse.svg");
        if (d.exists()) d.delete();
        
        try (FileOutputStream out = new FileOutputStream(d)) {
            new WorkflowUmlService(null).printWorkflowAsPlantUmlSvg(subject.getCheckWarehouse(), out);
        }
    }
}
