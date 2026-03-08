const dbName = process.env.DB_NAME || "benchmark";
const collName = process.env.COLL_NAME || "posts";
const total = parseInt(process.env.TOTAL_DOCS || "200000", 10);
const batchSize = parseInt(process.env.BATCH_SIZE || "5000", 10);

const database = db.getSiblingDB(dbName);
const coll = database.getCollection(collName);

print(`[seed] target=${dbName}.${collName}, total=${total}, batchSize=${batchSize}`);

coll.drop();

const now = Date.now();
let inserted = 0;

while (inserted < total) {
  const docs = [];
  const size = Math.min(batchSize, total - inserted);

  for (let i = 0; i < size; i++) {
    const idx = inserted + i;
    docs.push({
      title: `Post ${idx}`,
      body: `Synthetic benchmark payload ${idx}`,
      authorId: `user-${idx % 1000}`,
      category: ["news", "tech", "life", "sports"][idx % 4],
      score: idx % 100,
      createdAt: new Date(now - idx * 1000),
      updatedAt: new Date(now - idx * 1000),
      tags: [`tag-${idx % 10}`, `tag-${idx % 30}`],
      metadata: {
        source: "benchmark-seed",
        seq: idx,
      },
    });
  }

  coll.insertMany(docs, { ordered: false });
  inserted += size;

  if (inserted % (batchSize * 4) === 0 || inserted === total) {
    print(`[seed] inserted ${inserted}/${total}`);
  }
}

coll.createIndex({ createdAt: -1, _id: -1 });
print("[seed] done");
printjson(coll.stats({ scale: 1024 * 1024 }));
