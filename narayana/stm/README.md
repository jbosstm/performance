
mvn clean package
java -jar target/benchmarks.jar


I created 3 benchmarks (see https://github.com/jbosstm/performance/pull/185):
  LocalJTABenchmark.benchmark: starts a JTA txn and enlists a single XAResource and commits
  STMBenchmark.baseLineBenchmark: adds two integers together
  STMBenchmark.benchmark: starts an AtomicAction and increments and decrements two integers backed by transaction memory

I ran the benchmarks three times taking the best result from each.

Comparing the the % difference between the pr/2414 and main branches I found that

LocalJTABenchmark.benchmark: pr/2414 is 10% better
STMBenchmark.baseLineBenchmark pr/2414 is 0.2% worse
STMBenchmark.benchmark: pr/2414 is 0.25% worse

So if the LocalJTABenchmark result is repeatable then I think 10% gain is significant.

