# Introduction

The project is a library for load generating. The type of load is set by user, and the library provides a mechanism for user's load generating.

# System requirements

- JDK 11
- Gradle

# Working principles

The application allows you to generate load dynamically, according to the load profile set by the user. It supports ascending, descending and flat load graphs, and various types of their combinations, so that the user can create load scenarios that are close to real ones.
Each load graph have a next options:

- `Type` - ascending, descending or flat load type;
- `Duration` - duration of load graph in seconds;
- `Stretch factor` - changes the load range, and do a load graph a more or less steep;
- `Scale Factor` - scales the load linearly.

The generator is working by cycles, with a 1-second cycle time, according to a given load profile. The work performed by the generator is described in a methods of `LoadGeneratorJob` interface. This interface provides following methods:
- `onEach()` This method describes the basic unit of the work;
- `onBatch()` This method is called at the end of each load cycle time;
- `onEnd()` This method is called upon completion of the load profile work. 
  
The `onEach()` method is the main unit of work for a generator. He reflects the quantitative indicator of the load (operations per second). Other methods are used to perform additional flexible tasks (batch operations, aggregating operations, etc.), or for support tasks (logging, statistics, etc.).

During process and at the end of the generator work, user can download a result of the last load profile execution in the .csv format

# Configuration

Generator configuration is possible in auto-configuration mode if you are working with Spring Boot framework, or in manual configuration mode if you are not working with Spring Boot framework.
To get started you have to add the library dependency in your project. Next, you have to implement the `LoadGeneratorJob` interface, and  its required `onEach()` method.

### Auto-configuration mode

To activate the auto-configuration mode, you need to add `@LoadGeneratorAutoConfiguration` annotation to any class of your project.

### Manual configuration mode

For manual configuration, you need to create an instance of the `LoadGenerator` class and pass to constructor an `LoadGeneratorJob` interface implementation instance as argument

## Creating a load profile

After configuring the generator, you can start creating a load profile. It can be used in next ways:
- At runtime via web UI, if you are using Spring Boot Web;
- At runtime via REST endpoints, if you are using Spring Boot Web;
- At compile-time manually, using `createLoadProfile(...)` method from `LoadGenerator` class instance

### Using web UI

To start working with the generator web UI, you need to launch the application. The generator UI page is hosted at `{hostname}/load-generator/index.html`
This page provides elements for creating and managing a load profile with a visual display of the main parameters of load and controls for the load generator.

### Using REST endpoints

To start working with the generator REST endpoints, you need to launch the application. You can find documentation about REST endpoints at `{hostname}/swagger-ui/`. Note, that if you are using your own swagger configuration - you need to provide load generator REST endpoints yourself.
