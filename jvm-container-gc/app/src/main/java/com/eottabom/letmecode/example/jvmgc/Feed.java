package com.eottabom.letmecode.example.jvmgc;

import java.time.Instant;
import java.util.List;

/**
 * 응답으로 내려주는 게시글 피드.
 *
 * @param id 피드 번호
 * @param userId 피드를 요청한 사용자
 * @param createdAt 생성 시각
 * @param posts 게시글 목록
 */
public record Feed(long id, String userId, Instant createdAt, List<Post> posts) {

	/**
	 * 피드에 담기는 게시글.
	 *
	 * @param id 게시글 번호
	 * @param author 작성자
	 * @param title 제목
	 * @param summary 본문 요약
	 * @param likeCount 좋아요 수
	 */
	public record Post(String id, String author, String title, String summary, int likeCount) {

	}

}
