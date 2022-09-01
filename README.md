[![Java CI with Maven](https://github.com/sterlp/pmw/actions/workflows/maven.yml/badge.svg)](https://github.com/sterlp/pmw/actions/workflows/maven.yml)

# Poor Mans Workflow based on Quartz Scheduler

## Design-Goals

Build a very basic workflow `engine` which does only really basic stuff and is understood in a second.

- one simple jar to get it running
- no own deployment of a workflow server or any stuff
- reuse of a schedular framework
- be compatible to other frameworks
- Spring integration

## Spring setup

### Ensure quartz uses the spring transaction manager

By default this will be configured by spring using the `jdbc` store:

```yml
spring:
  quartz:
    job-store-type: jdbc
    overwrite-existing-jobs: true
```

### Maven

```xml
<dependency>
    <groupId>org.sterl.pmw</groupId>
    <artifactId>pmw-spring</artifactId>
    <version>1.0.0</version>
</dependency>

```

### Define a workflow

```java
@Service
@RequiredArgsConstructor
public class NewItemArrivedWorkflow {
    
    private final WarehouseService warehouseService;
    private final DiscountComponent discountComponent;
    private final WarehouseStockComponent createStock;
    private final UpdateInStockCountComponent updateStock;
    private final WorkflowService<JobDetail> workflowService;

    private Workflow<NewItemArrivedWorkflowState> w;
    private Workflow<NewItemArrivedWorkflowState> restorePriceWorkflow;
    

    @PostConstruct
    void createWorkflow() {
        w = Workflow.builder("check-warehouse", () -> NewItemArrivedWorkflowState.builder().build())
                .next(s -> createStock.checkWarehouseForNewStock(s.getItemId()))
                .next((s, c) -> {
                    final long stockCount = warehouseService.countStock(s.getItemId());
                    updateStock.updateInStockCount(s.getItemId(), stockCount);
                    
                    s.setWarehouseStockCount(stockCount);
                    
                    // check after a while if we have still so many items in stock
                    if (stockCount > 40) c.delayNextStepBy(Duration.ofMinutes(2));
                })
                .choose(s -> {
                        if (s.getWarehouseStockCount() > 40) return "discount-price";
                        else return "check-warehouse";
                    })
                    .ifSelected("discount-price", s -> {
                        var originalPrice = discountComponent.applyDiscount(s.getItemId(), s.getWarehouseStockCount());
                        s.setOriginalPrice(originalPrice);
                        
                        workflowService.execute(restorePriceWorkflow, s, Duration.ofMinutes(2));
                    })
                    .ifSelected("check-warehouse", s -> this.execute(s.getItemId()))
                    .build()
                .build();

        workflowService.register(w);
        
        restorePriceWorkflow = Workflow.builder("restore-item-price", () -> NewItemArrivedWorkflowState.builder().build())
                .next(s -> discountComponent.setPrize(s.getItemId(), s.getOriginalPrice()))
                .build();
        
        workflowService.register(restorePriceWorkflow);
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public String execute(long itemId) {
        return workflowService.execute(w, NewItemArrivedWorkflowState.builder()
                .itemId(itemId).build());
    }
}
```

### IDE

- eclipse plugin https://marketplace.eclipse.org/content/plantuml-plugin

## Looking for a real workflow engine

- https://camunda.com/
- https://cadenceworkflow.io/
- https://temporal.io/


- https://github.com/meirwah/awesome-workflow-engines