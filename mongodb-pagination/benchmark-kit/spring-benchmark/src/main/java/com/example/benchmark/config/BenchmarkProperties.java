package com.example.benchmark.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark")
public class BenchmarkProperties {

    private String collection = "posts";
    private int pageSize = 500;
    private List<Integer> pages = new ArrayList<>(List.of(0, 1, 10, 100, 300));
    // 측정 전에 두 방식을 미리 실행하는 횟수. 커넥션 풀 초기화, JIT 비용이 먼저 측정되는 쪽에 몰리지 않게 한다.
    private int warmup = 3;
    // 페이지마다 반복 측정하는 횟수. 결과는 중앙값을 쓴다.
    private int repeat = 5;

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getWarmup() {
        return warmup;
    }

    public void setWarmup(int warmup) {
        this.warmup = warmup;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public List<Integer> getPages() {
        return pages;
    }

    public void setPages(List<Integer> pages) {
        this.pages = pages;
    }
}
