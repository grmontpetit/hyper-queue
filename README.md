# HyperQueue

## About
This is a project that simulates an In-Memory Queue.

## How to run
### Prerequisites

- sbt
- scala

### Run the project

1. in the project's root folder, run
``` sbt run ```
2. Send requests to the server either with post (import the given postman collection) or with the curl scripts included.

## Completed features

All of the backend features have been implemented, except for the clean the older chunk feature.
There was not enough time to produce a front-end.

## Improvements

- The project doesn't have *any* unit tests / spray tests.
- The consumers ids are not properly tracked in the backend; only the nb. of consumers is tracked.
- The way the queue is accessed is not optimal because to consume 1 item, there is a lock (synchronize) happening everytime. A proper optimization would be to lock on multiple items within the queue based on how many consumers are registered.
- I left a few comments in the code as to where it must be made more elegant / better structured.