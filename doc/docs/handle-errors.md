In contrast to all other steps, the `onLastStepError` is called with the modified state as it was left by the last step. This allows custom code to pass any relevant state to the exception handler.

The error handler always terminates the workflow and is the final step to be executed.

```java
@Bean
simpleWorkflow() {
  returnWorkflow.builder("error flow", () ->  new SimpleWorkflowState())
    .next()
        .description("This step may fail")
            .function(s -> {})
            .build()
    .onLastStepError()
        .next()
            .description("Will only run if the last step **failed**")
            .function(s -> {})
            .build()
        .build()
    .next()
        .description("Will only run if **no** error occured")
        .function(s -> {})
        .build()
    .next()
        .description("Some other step")
        .function(s -> {})
        .build()
    .build();
}
```
