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

### Define a workflow

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateItemWorkflow {
    
    private final CreateStockComponent createStock;
    private final UpdateInStockCountComponent updateStock;
    private final WorkflowService<JobDetail> workflowService;

    private Workflow<CreateItemWorkflowContext> w;
    

    @PostConstruct
    void createWorkflow() {
        w = Workflow.builder("create-item", () -> CreateItemWorkflowContext.builder().build())
                .next(c -> {
                    c.setInStockCount(createStock.execute(c.getItemId()));
                })
                .next(c -> {
                    updateStock.updateInStockCount(c.getItemId(), c.getInStockCount());
                    log.info("");
                    c.setRetry(c.getRetry() + 1);
                    if (c.getRetry() < 10) throw new IllegalStateException("No " + c.getRetry());
                })
                .choose(c -> {
                    if (c.getInStockCount() > 50) return "large";
                    else return "small";
                }).ifSelected("large", c -> {
                    log.info("Created item {} with a large stock {}", c.getItemId(), c.getInStockCount());
                }).ifSelected("small", c -> {
                    log.info("Created item {} with a small stock", c.getItemId(), c.getInStockCount());
                }).build()
                .build();

        workflowService.register(w);
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public String execute(Item item) {
        return workflowService.execute(w, CreateItemWorkflowContext.builder()
                .itemId(item.getId()).build());
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