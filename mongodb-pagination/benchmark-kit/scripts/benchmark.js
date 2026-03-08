const dbName = process.env.DB_NAME || "benchmark";
const collName = process.env.COLL_NAME || "posts";
const pageSize = parseInt(process.env.PAGE_SIZE || "500", 10);
const pages = (process.env.PAGES || "0,1,10,100,300")
  .split(",")
  .map((v) => parseInt(v.trim(), 10))
  .filter((v) => !isNaN(v) && v >= 0);

const database = db.getSiblingDB(dbName);
const coll = database.getCollection(collName);

function runSkip(page) {
  const skipAmount = page * pageSize;
  const explain = coll
    .find({})
    .sort({ _id: -1 })
    .skip(skipAmount)
    .limit(pageSize)
    .explain("executionStats");

  return {
    page,
    skipAmount,
    ms: explain.executionStats.executionTimeMillis,
    docsExamined: explain.executionStats.totalDocsExamined,
    keysExamined: explain.executionStats.totalKeysExamined,
  };
}

function runRange(page) {
  const skipAmount = page * pageSize;
  if (skipAmount === 0) {
    return null;
  }

  const lastDoc = coll
    .find({})
    .sort({ _id: -1 })
    .skip(skipAmount - 1)
    .limit(1)
    .toArray()[0];

  const explain = coll
    .find({ _id: { $lt: lastDoc._id } })
    .sort({ _id: -1 })
    .limit(pageSize)
    .explain("executionStats");

  return {
    page,
    skipAmount,
    ms: explain.executionStats.executionTimeMillis,
    docsExamined: explain.executionStats.totalDocsExamined,
    keysExamined: explain.executionStats.totalKeysExamined,
  };
}

print(`[benchmark] target=${dbName}.${collName}, pageSize=${pageSize}, pages=${pages.join(",")}`);
print("page\tskip\tasIsMs\ttoBeMs\timprovement\tasIsDocsExamined\ttoBeDocsExamined");

let asIsTotalMs = 0;
let toBeTotalMs = 0;
let comparableCount = 0;

for (const page of pages) {
  const skipResult = runSkip(page);
  const rangeResult = runRange(page);

  const improvement =
    rangeResult && skipResult.ms > 0
      ? (((skipResult.ms - rangeResult.ms) / skipResult.ms) * 100).toFixed(2) + "%"
      : "-";

  print(
    `${page}\t${skipResult.skipAmount}\t${skipResult.ms}\t${rangeResult ? rangeResult.ms : "-"}\t${improvement}` +
      `\t${skipResult.docsExamined}\t${rangeResult ? rangeResult.docsExamined : "-"}`
  );

  if (rangeResult) {
    asIsTotalMs += skipResult.ms;
    toBeTotalMs += rangeResult.ms;
    comparableCount += 1;
  }
}

if (comparableCount > 0) {
  const asIsAvgMs = (asIsTotalMs / comparableCount).toFixed(3);
  const toBeAvgMs = (toBeTotalMs / comparableCount).toFixed(3);
  const avgImprovement = (((asIsTotalMs - toBeTotalMs) / asIsTotalMs) * 100).toFixed(2) + "%";
  print("");
  print(`[summary] comparablePages=${comparableCount}`);
  print(`[summary] AS-IS avg=${asIsAvgMs}ms`);
  print(`[summary] TO-BE avg=${toBeAvgMs}ms`);
  print(`[summary] improvement=${avgImprovement}`);
}
