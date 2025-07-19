package org.sterl.store.items.workflow;

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sterl.pmw.WorkflowUmlService;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.uml.PlantUmlWritter;
import org.sterl.store.items.component.DiscountComponent;
import org.sterl.store.items.component.UpdateInStockCountComponent;
import org.sterl.store.items.component.WarehouseStockComponent;
import org.sterl.store.warehouse.WarehouseService;

@ExtendWith(MockitoExtension.class)
class NewItemArrivedWorkflowMockTest {

    private NewItemArrivedWorkflow subject = new NewItemArrivedWorkflow();

    WorkflowRepository repo = new WorkflowRepository();
    WorkflowUmlService umlService = new WorkflowUmlService(repo);
    
    Workflow<NewItemArrivedState> restorePriceWorkflow;
    Workflow<NewItemArrivedState> checkWarehouseWorkflow;

    @BeforeEach
    void setUp() throws Exception {
        repo.clear();
        restorePriceWorkflow = subject.restorePriceWorkflow(mock(DiscountComponent.class));
        checkWarehouseWorkflow = subject.checkWarehouseWorkflow(
                mock(WarehouseService.class), 
                mock(UpdateInStockCountComponent.class),
                mock(WarehouseStockComponent.class),
                mock(DiscountComponent.class),
                restorePriceWorkflow);
        
        repo.register("restorePriceWorkflow", restorePriceWorkflow);
        repo.register("checkWarehouseWorkflow", checkWarehouseWorkflow);
    }

    @Test
    void test() throws Exception {

        PlantUmlWritter.writeAsPlantUmlSvg("./check-warehouse.svg", checkWarehouseWorkflow, umlService);
    }
    
    @Test
    void testString() throws Exception {
        System.err.println(
                umlService.printWorkflow(checkWarehouseWorkflow)
        );
        //subject.addWorkflow(w, result);
        //final String diagram = result.build()
    }
    
}
