package com.example.benchmark.runner;

import com.example.benchmark.config.BenchmarkProperties;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class PaginationBenchmarkRunner implements CommandLineRunner {

    private static final DecimalFormat DF = new DecimalFormat("0.000");

    private final MongoTemplate mongoTemplate;
    private final BenchmarkProperties properties;

    public PaginationBenchmarkRunner(MongoTemplate mongoTemplate, BenchmarkProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        List<Integer> pages = new ArrayList<>(properties.getPages());
        pages.removeIf(page -> page == null || page < 0);

        if (pages.isEmpty()) {
            System.out.println("[error] benchmark.pages must contain non-negative integers");
            return;
        }

        pages.sort(Comparator.naturalOrder());

        int pageSize = properties.getPageSize();
        String collection = properties.getCollection();
        int maxPage = pages.get(pages.size() - 1);

        long totalDocs = mongoTemplate.getCollection(collection).estimatedDocumentCount();
        System.out.printf("[spring-benchmark] target=%s, totalDocs=%d%n", collection, totalDocs);
        System.out.printf("[spring-benchmark] pageSize=%d, pages=%s%n", pageSize, pages);

        // TO-BE(cursor): 이전 페이지의 lastId를 이어받아 순차 조회
        Map<Integer, ToBeResult> toBeMap = new HashMap<>();
        ObjectId lastId = null;
        double cumulative = 0.0;

        for (int page = 0; page <= maxPage; page++) {
            Query query = new Query()
                    .with(Sort.by(Sort.Direction.DESC, "_id"))
                    .limit(pageSize);

            if (lastId != null) {
                query.addCriteria(Criteria.where("_id").lt(lastId));
            }

            long started = System.nanoTime();
            List<Document> docs = mongoTemplate.find(query, Document.class, collection);
            double stepMs = nanosToMs(System.nanoTime() - started);
            cumulative += stepMs;

            if (docs.isEmpty()) {
                break;
            }

            lastId = docs.get(docs.size() - 1).getObjectId("_id");
            toBeMap.put(page, new ToBeResult(stepMs, cumulative));
        }

        System.out.println("page\tskip\tasIsMs\ttoBeStepMs\ttoBeCumMs\timprovement(step)\timprovement(cum)");

        double asIsSum = 0.0;
        double toBeStepSum = 0.0;
        double toBeCumSum = 0.0;
        int comparable = 0;

        for (int page : pages) {
            int skip = page * pageSize;

            Query asIsQuery = new Query()
                    .with(Sort.by(Sort.Direction.DESC, "_id"))
                    .skip(skip)
                    .limit(pageSize);

            long started = System.nanoTime();
            mongoTemplate.find(asIsQuery, Document.class, collection);
            double asIsMs = nanosToMs(System.nanoTime() - started);

            ToBeResult toBe = toBeMap.get(page);
            if (toBe == null) {
                System.out.printf("%d\t%d\t%s\t-\t-\t-\t-%n", page, skip, DF.format(asIsMs));
                continue;
            }

            String stepImprovement = improvement(asIsMs, toBe.stepMs());
            String cumImprovement = improvement(asIsMs, toBe.cumulativeMs());

            System.out.printf(
                    "%d\t%d\t%s\t%s\t%s\t%s\t%s%n",
                    page,
                    skip,
                    DF.format(asIsMs),
                    DF.format(toBe.stepMs()),
                    DF.format(toBe.cumulativeMs()),
                    stepImprovement,
                    cumImprovement);

            asIsSum += asIsMs;
            toBeStepSum += toBe.stepMs();
            toBeCumSum += toBe.cumulativeMs();
            comparable++;
        }

        if (comparable > 0) {
            System.out.println();
            System.out.printf("[summary] comparablePages=%d%n", comparable);
            System.out.printf("[summary] AS-IS avg=%sms%n", DF.format(asIsSum / comparable));
            System.out.printf("[summary] TO-BE step avg=%sms%n", DF.format(toBeStepSum / comparable));
            System.out.printf("[summary] TO-BE cumulative avg=%sms%n", DF.format(toBeCumSum / comparable));
            System.out.printf("[summary] step improvement=%s%n", improvement(asIsSum, toBeStepSum));
            System.out.printf("[summary] cumulative improvement=%s%n", improvement(asIsSum, toBeCumSum));
        }
    }

    private static double nanosToMs(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static String improvement(double asIs, double toBe) {
        if (asIs <= 0) {
            return "-";
        }
        double ratio = ((asIs - toBe) / asIs) * 100.0;
        return DF.format(ratio) + "%";
    }

    private record ToBeResult(double stepMs, double cumulativeMs) {
    }
}
