```java
Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", SimpleWorkflowState::new)
    .next(s -> {})
    .next(s -> {})
    .next()
        .function(e -> {})
        .transactional(false)
        .build()
    .build();
```

![Simple Workflow](./assets/simple-workflow.svg)
