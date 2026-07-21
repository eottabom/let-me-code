package com.eottabom.letmecode.example.mongodb;

import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * TO-BE: _id 기반 cursor(range query) pagination. offset 과 무관하게 항상 일정한 성능을 유지한다.
 */
public class CursorPaginationRepository {

	private final MongoTemplate mongoTemplate;

	private final String collectionName;

	public CursorPaginationRepository(MongoTemplate mongoTemplate, String collectionName) {
		this.mongoTemplate = mongoTemplate;
		this.collectionName = collectionName;
	}

	public List<Document> findByLastId(String lastId, int size) {
		Query query = new Query().with(Sort.by(Sort.Direction.DESC, "_id")).limit(size);

		if (lastId != null) {
			query.addCriteria(Criteria.where("_id").lt(new ObjectId(lastId)));
		}

		return mongoTemplate.find(query, Document.class, collectionName);
	}

	/**
	 * 전체 문서를 cursor 방식으로 순회한다. (엑셀 다운로드 등)
	 */
	public List<Document> findAll(int batchSize) {
		List<Document> all = new ArrayList<>();
		String lastId = null;

		while (true) {
			List<Document> batch = findByLastId(lastId, batchSize);
			if (batch.isEmpty()) {
				break;
			}
			all.addAll(batch);
			lastId = batch.getLast().getObjectId("_id").toHexString();
		}

		return all;
	}

}
