# CHANGELOG
## SDK v0.6.5
- Fixed bugs related to complicated inheritance patterns involving interfaces.
- Resolved issues where abstract methods were duplicated and selectively logged, which caused child classes to miss inherited methods from parent classes.

## SDK v0.6.6
- Addressed a bug where interfaces were breaking due to HashMap injection in the class bytecode. Such interfaces are now left unmodified by the SDK.

## SDK v0.6.7
- Fixed an issue where the JVM was incorrectly resolving the parent class of some methods injected by the SDK.

## SDK v0.6.8
- The state of method call is now managed in the Runtime class of the SDK instead of the user's class. This reduces the amount of code that needs to be injected into the user's code and further simplifies the SDK architecture.

## SDK v0.6.9
- Improved transaction management in Hibernate methods. Previously, users had to add a Transactional annotation for methods that were lazy loading data from the database for mocks to work correctly with Direct Invoke. This is now handled by the SDK without requiring changes to user code.

## SDK v0.7.0
- Introduced selective logging modes, allowing the SDK to probe only specific classes and methods in the user's code.
- These Methods can now be defined using annotations at compile time for selective logging.
- Added a new mode that only logs selected methods and their child calls to other methods.
- Changed the data type of method call state in Runtime class from int to long, ensuring correct state management for higher number of method calls.

## SDK v0.7.1
- Minor fixes to the SDK.

## SDK v0.7.2
- Fixed issues with circular POJO parsing. Users previously had to import packages from SDK's shaded JAR for mocks to work with Direct Invoke. 
- Now, users can import Jackson's package for parsing POJOs.

## SDK v0.7.3
- Fixed an issue where some methods when invoked were creating two test candidates. This was happening for methods where a synthetic method was emitted by the Java compiler.
