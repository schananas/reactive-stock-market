# Reactive Stock Exchange

### Introduction

This project demonstrates reactive implementation of simple stock exchange platform.
Originally assigment is given to Senior Software Engineers as a technical code interview in some stock/crypto exchange companies.

Read original [system requirements here.](system_requirements.pdf)

Takeaways of implementation:
- Spring Boot application with matching engine as it core
- Custom Reactive CQRS framework
- Matching engine implemented using Max-Heap and Min-Heap
- Application supports backpressure and event streaming

### Implementation

- **Matching engine**
    + Matching engine uses Max-Heap and Min-Heap
    + Time complexity for critical operations are as:
        + Add – O(log N)
        + Cancel – O(1)
    + Buy tree - The collection of orders sorted in the ascending order, that is, higher buy prices have priority to be matched over lower.
    + Sell tree - The collection of orders in the descending order, that is, lower sell prices have priority to be matched over higher.
    + Each state transition is the consequence of an event. Events are played sequentially and therefore engine is single-threaded. Thread synchronisation is handled outside of engine.

- **Reactive**
    + Asynchronous, event driven, non-blocking programming perfectly fits for given problem. We want to subscribe to engine updates instead of blocking the threads.
    + Business logic can be broken down into a pipeline of steps where each of the steps can be executed asynchronously
    + Using Reactor most parallelism and concurrency in project is carefully handled.
    + Operations are optimised to execute in parallel when possible. For example orders of single asset are executed sequentially, but orders of different assets are executed in parallel.
    + Scalability: Thread-based architectures just don’t scale that well. There is a limit to how many threads can be created, fit into memory and managed at a time. The reactor pattern removes this problem by simply demultiplexing concurrent incoming requests and running them, on a (usually) single-application thread.


- **CQRS**
    + Greatly simplifies architecture, scalability and modularity
    + Model order requests as commands. Command handlers validates request and then either accepts or reject order request.
    + All state changes can be modeled as an event, and can be stored and replayed if needed. Lack of concurrency between events also ensures determinism and makes code much cleaner and more efficient to run.
    + This avoids need to store complicated states and structures in database, instead if needed in-memory state can be reconstructed based on past events.
    + Use projections to separate aggregate state and state used for querying. We are able to build highly optimised projections based on business needs. We can query them without need to peek into engine state, and avoid potential performance congestions.


- **Protobuf**
    + Describes API schema of business objects once, in proto format (see [api.proto](src/main/resources/api.proto))
    + Supports backward compatibility
    + The Protocol Buffers specification is implemented in many languages
    + Less Boilerplate Code - auto generated code, out-of-box JSON serializer...

### Tests

- [x] Unit tests
- [x] Integration tests
- [x] Load test (stress test)

### How to run

Execute `mvn clean install` to build project and generate protobuf classes.

Then execute `mvn spring-boot:run` to run application, or use IDE of choice to run application as Spring Boot application.