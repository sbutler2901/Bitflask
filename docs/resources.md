# Resources

## Libraries

- [Guice](https://github.com/google/guice)
- [Guava](https://github.com/google/guava)
- [Mug](https://github.com/google/mug)
- [Truth](https://github.com/google/truth)
- [Flogger](https://github.com/google/flogger)
- [JCommander](https://jcommander.org/)

## Tools

### Docker

- [Dockerfile Reference](https://docs.docker.com/engine/reference/builder/)
- [Multi-stage Builds](https://docs.docker.com/build/building/multi-stage/)
- [DockerHub - Eclipse Temurin](https://hub.docker.com/_/eclipse-temurin)
- [DockerHub - Gradle](https://hub.docker.com/_/gradle)
- [IntelliJ Docker](https://www.jetbrains.com/help/idea/docker.html)

### Gradle

- [Dockerfile](https://github.com/keeganwitt/docker-gradle)
- [CLI Interface](https://docs.gradle.org/current/userguide/command_line_interface.html)
- [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)

## Articles

- [Java theory and practice: Thread pools and work queues, Brian Goetz](https://codeantenna.com/a/B2xXjD1Hql)
- [Guava Service Examples](https://cdap.atlassian.net/wiki/spaces/CE/pages/1595185014/Guava+services+tutorial+with+examples)
- [Data-Oriented Programming, Brian Goetz](https://www.infoq.com/articles/data-oriented-programming-java/)

## Notes

- I/O bound tasks optimum number of threads
    - `threads = number of cores * (1 + wait time / service time)`
    - [SO1](https://stackoverflow.com/a/13958877)
    - [SO2](https://stackoverflow.com/a/62556865)
- Immutable collection best practices
    - [As method parameter / return type](https://stackoverflow.com/questions/9519105/use-of-guava-immutable-collection-as-method-parameter-and-or-return-type)
