package com.eottabom.letmecode.example.jvmgc;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 요청마다 게시글 피드를 만들고 최근 피드를 캐시해 Young/Old 영역에 모두 부하를 준다.
 */
@RestController
public class FeedController {

	private final int postCount;

	private final AtomicReferenceArray<Feed> recentFeeds;

	private final AtomicLong sequence = new AtomicLong();

	public FeedController(@Value("${demo.feed.posts}") int postCount,
			@Value("${demo.feed.retained}") int retainedCount) {
		this.postCount = postCount;
		this.recentFeeds = new AtomicReferenceArray<>(retainedCount);
	}

	@GetMapping("/feed")
	public Feed feed() {
		long id = this.sequence.incrementAndGet();
		ThreadLocalRandom random = ThreadLocalRandom.current();
		List<Feed.Post> posts = new ArrayList<>(this.postCount);
		for (int index = 0; index < this.postCount; index++) {
			long postId = random.nextLong(1_000_000);
			posts.add(new Feed.Post("post-" + postId, "user-" + random.nextInt(10_000), "게시글 " + postId,
					"게시글 " + postId + "의 본문 요약", random.nextInt(1_000)));
		}
		Feed feed = new Feed(id, "user-" + random.nextInt(10_000), Instant.now(), posts);
		// 캐시처럼 최근 피드를 일정 개수 유지해 생존 객체를 만든다.
		this.recentFeeds.set((int) (id % this.recentFeeds.length()), feed);
		return feed;
	}

	@GetMapping("/runtime")
	public Map<String, Object> runtime() {
		Runtime runtime = Runtime.getRuntime();
		List<String> collectors = ManagementFactory.getGarbageCollectorMXBeans()
			.stream()
			.map(GarbageCollectorMXBean::getName)
			.toList();
		return Map.of("availableProcessors", runtime.availableProcessors(), "maxHeapBytes", runtime.maxMemory(),
				"garbageCollectors", collectors);
	}

}
