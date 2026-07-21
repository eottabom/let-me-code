package com.eottabom.letmecode.example.mongodb;

import java.util.List;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * AS-IS: skip + limit 기반 offset pagination. page 번호가 커질수록 skip 값이 증가하여 성능이 저하된다.
 */
public class SkipPaginationRepository {

	private final MongoTemplate mongoTemplate;

	private final String collectionName;

	public SkipPaginationRepository(MongoTemplate mongoTemplate, String collectionName) {
		this.mongoTemplate = mongoTemplate;
		this.collectionName = collectionName;
	}

	public List<Document> findByPage(int page, int size) {
		Query query = new Query().with(Sort.by(Sort.Direction.DESC, "_id")).with(PageRequest.of(page, size));

		return mongoTemplate.find(query, Document.class, collectionName);
	}

}
