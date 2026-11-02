package com.example.benchmark.runner;

import com.example.benchmark.config.BenchmarkProperties;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

        int warmup = Math.max(0, properties.getWarmup());
        int repeat = Math.max(1, properties.getRepeat());
        System.out.printf("[spring-benchmark] warmup=%d, repeat=%d (median)%n", warmup, repeat);

        // 페이지마다 cursor 의 시작점(이전 페이지의 마지막 _id)을 시간 측정 없이 미리 구해둔다.
        List<ObjectId> cursorStarts = new ArrayList<>();
        ObjectId lastId = null;
        for (int page = 0; page <= maxPage; page++) {
            List<Document> docs = mongoTemplate.find(cursorQuery(lastId, pageSize), Document.class, collection);
            if (docs.isEmpty()) {
                break;
            }
            cursorStarts.add(lastId);
            lastId = docs.get(docs.size() - 1).getObjectId("_id");
        }

        // 먼저 실행되는 쪽에 커넥션 풀 초기화, JIT 비용이 몰리지 않도록 두 방식을 똑같이 미리 돌린다.
        for (int i = 0; i < warmup; i++) {
            for (int page = 0; page < Math.min(2, cursorStarts.size()); page++) {
                mongoTemplate.find(skipQuery(page * pageSize, pageSize), Document.class, collection);
                mongoTemplate.find(cursorQuery(cursorStarts.get(page), pageSize), Document.class, collection);
            }
        }

        // TO-BE(cursor): 페이지마다 repeat 번 재서 중앙값을 쓰고, 누적 시간은 중앙값을 더한다.
        Map<Integer, ToBeResult> toBeMap = new HashMap<>();
        double cumulative = 0.0;
        for (int page = 0; page < cursorStarts.size(); page++) {
            Query query = cursorQuery(cursorStarts.get(page), pageSize);
            double stepMs = medianMs(() -> mongoTemplate.find(query, Document.class, collection), repeat);
            cumulative += stepMs;
            toBeMap.put(page, new ToBeResult(stepMs, cumulative));
        }

        System.out.println("page\tskip\tasIsMs\ttoBeStepMs\ttoBeCumMs\timprovement(step)\timprovement(cum)");

        double asIsSum = 0.0;
        double toBeStepSum = 0.0;
        double toBeCumSum = 0.0;
        int comparable = 0;

        for (int page : pages) {
            int skip = page * pageSize;

            Query asIsQuery = skipQuery(skip, pageSize);
            double asIsMs = medianMs(() -> mongoTemplate.find(asIsQuery, Document.class, collection), repeat);

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

    private static Query skipQuery(int skip, int pageSize) {
        return new Query()
                .with(Sort.by(Sort.Direction.DESC, "_id"))
                .skip(skip)
                .limit(pageSize);
    }

    private static Query cursorQuery(ObjectId lastId, int pageSize) {
        Query query = new Query()
                .with(Sort.by(Sort.Direction.DESC, "_id"))
                .limit(pageSize);
        if (lastId != null) {
            query.addCriteria(Criteria.where("_id").lt(lastId));
        }
        return query;
    }

    private static double medianMs(Runnable action, int repeat) {
        double[] samples = new double[repeat];
        for (int i = 0; i < repeat; i++) {
            long started = System.nanoTime();
            action.run();
            samples[i] = nanosToMs(System.nanoTime() - started);
        }
        Arrays.sort(samples);
        return (repeat % 2 == 1) ? samples[repeat / 2] : (samples[repeat / 2 - 1] + samples[repeat / 2]) / 2.0;
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
