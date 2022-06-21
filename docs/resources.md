# Resources

## Articles

- [Java theory and practice: Thread pools and work queues](https://codeantenna.com/a/B2xXjD1Hql) -
  Brian Goetz

## Notes

- I/O bound tasks optimum number of threads
  - `threads = number of cores * (1 + wait time / service time)`
  - [SO1](https://stackoverflow.com/a/13958877)
  - [SO2](https://stackoverflow.com/a/62556865)