[![Java CI with Maven](https://github.com/sterlp/pmw/actions/workflows/maven.yml/badge.svg)](https://github.com/sterlp/pmw/actions/workflows/maven.yml)

# Poor Mans Workflow based on Quartz Scheduler

## Design-Goals

Build a very basic workflow `engine` which does only really basic stuff and is understood in a second.

- one simple jar to get it running
- no own deployment of a workflow server or any stuff
- reuse of a scheduler framework
- be compatible to other frameworks
- Spring integration

## ToDo

- [x] First Quartz integration
- [x] First Spring integration
- [x] First PlantUML integration
- [x] Wait as own step
- [x] Trigger workflows in an own step
- [x] Link to workflows in repository using `trigger->`
- [ ] Logging & Audit & History Plugin
- [ ] Standalone flow version using a single thread
- [ ] Support multiple sub steps if choose

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

Select latest version: https://search.maven.org/search?q=a:pmw-spring

```xml
<dependency>
    <groupId>org.sterl.pmw</groupId>
    <artifactId>pmw-spring</artifactId>
    <version>1.x.x</version>
</dependency>

```

### Define a workflow

![check-warehouse](/example/check-warehouse.svg)

```java
@Service
@RequiredArgsConstructor
public class NewItemArrivedWorkflow {
    
    private final WarehouseService warehouseService;
    private final DiscountComponent discountComponent;
    private final WarehouseStockComponent createStock;
    private final UpdateInStockCountComponent updateStock;
    private final WorkflowService<JobDetail> workflowService;

    @Getter
    private Workflow<NewItemArrivedWorkflowState> checkWarehouse;
    @Getter
    private Workflow<NewItemArrivedWorkflowState> restorePriceSubWorkflow;
    

    @PostConstruct
    void createWorkflow() {
        checkWarehouse = Workflow.builder("check-warehouse", () -> NewItemArrivedWorkflowState.builder().build())
                .next("check warehouse for new stock", s -> createStock.checkWarehouseForNewStock(s.getItemId()))
                .next("update item stock", (s, c) -> {
                    final long stockCount = warehouseService.countStock(s.getItemId());
                    updateStock.updateInStockCount(s.getItemId(), stockCount);
                    
                    s.setWarehouseStockCount(stockCount);
                })
                .sleep("Wait if stock is > 40", (s) -> s.getWarehouseStockCount() > 40 ? Duration.ofMinutes(2) : Duration.ZERO)
                .choose("check stock", s -> {
                        if (s.getWarehouseStockCount() > 40) return "discount-price";
                        else return "check-warehouse-again";
                    })
                    .ifSelected("discount-price", "> 40", s -> {
                        var originalPrice = discountComponent.applyDiscount(s.getItemId(), s.getWarehouseStockCount());
                        s.setOriginalPrice(originalPrice);
                        
                        workflowService.execute(restorePriceSubWorkflow, s, Duration.ofMinutes(2));
                    })
                    .ifSelected("trigger->restore-item-price", "< 40", s -> this.execute(s.getItemId()))
                    .build()
                .build();

        workflowService.register(checkWarehouse);
        
        restorePriceSubWorkflow = Workflow.builder("restore-item-price", () -> NewItemArrivedWorkflowState.builder().build())
                .next("set price from workflow state", s -> discountComponent.setPrize(s.getItemId(), s.getOriginalPrice()))
                .build();
        
        workflowService.register(restorePriceSubWorkflow);
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public String execute(long itemId) {
        return workflowService.execute(checkWarehouse, NewItemArrivedWorkflowState.builder()
                .itemId(itemId).build());
    }
}
```
### Export Workflow as UML

```java
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
    void testPrintSimple() throws Exception {
        SerializationUtil.writeAsPlantUmlSvg("./check-warehouse.svg", subject.getCheckWarehouse());
    }
    
    @Test
    void testPrintWithSubworkflowSupportByName() throws Exception {
        WorkflowRepository repo = new WorkflowRepository();
        WorkflowUmlService umlService = new WorkflowUmlService(repo);
        
        repo.register(subject.getCheckWarehouse());
        repo.register(subject.getRestorePriceSubWorkflow());

        SerializationUtil.writeAsPlantUmlSvg("./check-warehouse.svg", subject.getCheckWarehouse().getName(), umlService);
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