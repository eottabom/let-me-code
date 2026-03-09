

### logger disabled vs logger enabled vs system.out.println

| Benchmark                               |Mode|Cnt| Score    | Error           | Units |
|-----------------------------------------|--|--|----------|-----------------|-------|
| LoggingBenchmark.benchmarkJavaLoggerDisabled | thrpt| 10 | 2618.238 | ±  90.077  | ops/s |
| LoggingBenchmark.benchmarkJavaLoggerEnabled  | thrpt| 10   | 2.986 | ±   0.711  | ops/s |
| LoggingBenchmark.benchmarkLoggerDisabled     | thrpt| 10 | 2480.615 | ± 272.262 | ops/s |
| LoggingBenchmark.benchmarkLoggerEnabled      | thrpt| 10 | 1419.497 | ± 221.123 | ops/s |
| LoggingBenchmark.benchmarkSystemOutPrintln   | thrpt| 10  |    6.468 | ±   0.692 | ops/s |
