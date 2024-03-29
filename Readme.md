![img.png](readme_logo.png)
# Introduction

The project is a library for flexible load generating. Generator emulates the real world load based on time intervals and gives you the opportunity to test your system under volatile load such as website traffic peaks or batch requests.

![img.png](readme_overview.png)

# System requirements

- JDK 11
- Gradle
- Spring Boot

# Working principles

The application allows you to generate load dynamically, according to the load profile set by the user. The load profile is set by the user through the UI interface by drawing a load graph on the coordinate grid.

The generator is working by cycles, with a 1-second cycle time, according to a given load profile. The work performed by the generator is described in a methods of `LoadGeneratorJob` interface. This interface provides following methods:
- `onEach(): Boolean` This method describes the basic unit of the work;
- `onStart()` This method is called once before starting load profile work;
- `onFinish()` This method is called once upon completion of the load profile work. 
  
The `onEach(): Boolean` method is the main unit of work for a generator. It reflects the quantitative indicator of the load (operations per second). Other methods are used to perform additional flexible tasks (preparation tasks, warming up, aggregating operations, etc.), or for support tasks (logging, statistics, etc.).

# Quick start

## Dependency

If you are using Gradle in your app you can add library as dependency following one of the next steps:

Using JitPack: https://jitpack.io/#stroiker/flexible-load-generator

[![](https://jitpack.io/v/stroiker/flexible-load-generator.svg)](https://jitpack.io/#stroiker/flexible-load-generator)

OR

Using Git Source Control:
1) Add to `settings.gradle` additional source mapping
```
sourceControl {
    gitRepository("https://github.com/stroiker/flexible-load-generator.git") {
        producesModule("me.stroiker:flexible-load-generator")
    }
}
```
2) Add to `build.gradle` library dependency
```
dependencies {
    ...
    implementation "me.stroiker:flexible-load-generator:${version}"
}
```
3) Run Gradle task `assemble` to generate source classes.

Example of app with library you can find here https://github.com/stroiker/flexible-load-generator-example

## Configuration

Generator configuration managed by auto-configuration mode inside Spring Boot framework context. To get started you need to add this library to your app classpath.
Then you have to annotate any bean with `@LoadGeneratorAutoConfiguration` annotation, then implement the `LoadGeneratorJob` interface. Your implementation must be Spring bean.

If your business logic takes a certain amount of time, and generator cannot provide the required performance with default settings - you can set in your `application.properties` parameter `load-generator.threadPoolSize`
which will increase the parallelism level of generator and, accordingly, your target performance. Default value of this parameter is according to number of logical cores on your CPU.

## Creating a load profile

To start working with the generator web UI, you need to launch the application. The generator UI page is opened at `http://localhost:8080`.
You can change UI page path using spring boot property `spring.mvc.static-path-pattern` in your `application.properties` file. This page provides elements for creating and managing a load profile. 
To create a load profile, you need to press the left mouse button inside the coordinate grid and drag the cursor, drawing the required load profile, 
in accordance with the OPS (y scale) and time (x scale) scales. You can also adjust the scaling of each scale with the corresponding buttons.
After you chose load profile you can start generator by `START` button.

## Measurements

Flexible load generator provides next types of measure metrics:
- Success/failed invocations count. Method `onEach(): Boolean` can return result of invocation as Boolean type. 
Results are calculated for each type and can be viewed on additional chart. Exceptions thrown during invocation interprets as `false` result on chart;
- Invocation latency. Latency is calculated for each invocation result type up to 1 microsecond precision 
and measured as trimmed mean `tm90` metric, which calculates the average after removing the 10% of data points with the highest values;
- Operations per second. Is calculated as expected(planned) and actual OPS.

All metrics are available after load generation complete by appropriate checkboxes on chart.


