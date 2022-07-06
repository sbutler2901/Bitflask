# Resources

## Libraries

- [Guice](https://github.com/google/guice)
- [Guava](https://github.com/google/guava)
- [Flogger](https://github.com/google/flogger)

## Articles

- [Java theory and practice: Thread pools and work queues, Brian Goetz](https://codeantenna.com/a/B2xXjD1Hql)
- [Guava Service Examples](https://cdap.atlassian.net/wiki/spaces/CE/pages/1595185014/Guava+services+tutorial+with+examples)

## Notes

- I/O bound tasks optimum number of threads
    - `threads = number of cores * (1 + wait time / service time)`
    - [SO1](https://stackoverflow.com/a/13958877)
    - [SO2](https://stackoverflow.com/a/62556865)
- Immutable collection best practices
    - [As method parameter / return type](https://stackoverflow.com/questions/9519105/use-of-guava-immutable-collection-as-method-parameter-and-or-return-type)
