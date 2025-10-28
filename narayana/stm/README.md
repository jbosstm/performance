This module contains three benchmarks:

  - LocalJTABenchmark.benchmark: starts a JTA txn and enlists a single XAResource and commits
  - STMBenchmark.baseLineBenchmark: adds two integers together
  - STMBenchmark.benchmark: starts an AtomicAction and increments and decrements two integers backed by transaction memory

Run the benchmarks using the command `java -jar target/benchmarks.jar`