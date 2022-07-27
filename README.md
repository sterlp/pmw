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