[![Java CI with Maven](https://github.com/sterlp/pmw/actions/workflows/maven.yml/badge.svg)](https://github.com/sterlp/pmw/actions/workflows/maven.yml)

# Poor Mans Workflow based on Quartz Scheduler

## Design-Goals

Build a very basic workflow `engine` which does only really basic stuff and is understood in a second.

-   one simple jar to get it running
-   no own deployment of a workflow server or any stuff
-   Spring integration

## ToDo

-   [x] First Spring integration
-   [x] First PlantUML integration
-   [x] Wait as own step
-   [x] Trigger workflows in an own step
-   [x] Link to workflows in repository using `trigger->`
-   [ ] Support multiple sub steps if choose

### Maven

Select latest version: https://central.sonatype.com/search?q=g%3Aorg.sterl.pmw

```xml
<dependency>
    <groupId>org.sterl.pmw</groupId>
    <artifactId>spring-pmw-core</artifactId>
    <version>2.x.x</version>
</dependency>
```

### Define a workflow

![check-warehouse](/example/check-warehouse.svg)

```java
@Configuration
public class NewItemArrivedWorkflow {

    @Bean
    Workflow<NewItemArrivedState> restorePriceWorkflow(DiscountComponent discountComponent) {
        return Workflow.builder("Restore Item Price", () -> NewItemArrivedState.builder().build())
                .next("updatePrice", c -> discountComponent.setPrize(c.data().getItemId(), c.data().getOriginalPrice()))
                .next("sendMail", s -> {})
                .build();
    }

    @Bean
    Workflow<NewItemArrivedState> checkWarehouseWorkflow(
            WarehouseService warehouseService,
            UpdateInStockCountComponent updateStock,
            WarehouseStockComponent createStock,
            DiscountComponent discountComponent,
            Workflow<NewItemArrivedState> restorePriceWorkflow) {

        return Workflow.builder("Check Warehouse", () -> NewItemArrivedState.builder().build())
                .next().description("check warehouse for new stock")
                    .function(c -> createStock.checkWarehouseForNewStock(c.data().getItemId()))
                    .build()
                .next("update item stock", c -> {
                    final var s = c.data();
                    final long stockCount = warehouseService.countStock(s.getItemId());
                    updateStock.updateInStockCount(s.getItemId(), stockCount);
                    s.setWarehouseStockCount(stockCount);
                })
                .sleep("wait", "Wait if stock is > 40", (s) -> s.getWarehouseStockCount() > 40 ? Duration.ofMinutes(2) : Duration.ZERO)
                .choose("check stock", s -> {
                        if (s.getWarehouseStockCount() > 40) return "discount-price";
                        else return "buy-new-items";
                    })
                    .ifTrigger("discount-price", restorePriceWorkflow)
                        .description("WarehouseStockCount > 40")
                        .delay(Duration.ofMinutes(2))
                        .function(s -> {
                            var originalPrice = discountComponent.applyDiscount(s.getItemId(), 
                                    s.getWarehouseStockCount());
                            s.setOriginalPrice(originalPrice);
                            return s;
                        }).build()
                    .ifSelected("buy-new-items", c -> {})
                    .build()
                .build();
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

## Use the UI

### Maven

```xml
<dependency>
    <groupId>org.sterl.pmw</groupId>
    <artifactId>spring-pmw-core</artifactId>
    <version>2.x.x</version>
</dependency>
```

### Spring

```java
@EnableWorkflows
@EnableWorkflowsUI
@SpringBootApplication
public class StoreApplication {
```

### Preview

-   http://localhost:8080/pmw-ui

![PMW Admin Dashboard](/pmw-admin-dashboard-ui.png)

### IDE

-   eclipse plugin https://marketplace.eclipse.org/content/plantuml-plugin

## Looking for a real workflow engine

-   https://github.com/quartz-scheduler/quartz
-   https://camunda.com/
-   https://cadenceworkflow.io/
-   https://temporal.io/

-   https://github.com/meirwah/awesome-workflow-engines
