package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ElasticsearchIndexingJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexingJob.class);

    private final MongoTemplate mongoTemplate;
    private final RestTemplate restTemplate;

    @Value("${app.elasticsearch.url:}")
    private String elasticsearchUrl;

    public ElasticsearchIndexingJob(DistributedLockService distributedLockService,
                                     JobMonitorService jobMonitorService,
                                     MongoTemplate mongoTemplate,
                                     RestTemplate restTemplate) {
        super(distributedLockService, jobMonitorService);
        this.mongoTemplate = mongoTemplate;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getJobName() {
        return "elasticsearch-indexing";
    }

    @Override
    public String getJobGroup() {
        return "search";
    }

    @Override
    public String getDescription() {
        return "Indexes MongoDB documents into Elasticsearch for full-text search";
    }

    @Override
    public int getLockTtlSeconds() {
        return 600;
    }

    @Override
    public int getMaxRetries() {
        return 2;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.elasticsearch-indexing.cron:0 */30 * * * *}")
    public void run() {
        super.run();
    }

    @Override
    protected void execute() {
        if (elasticsearchUrl == null || elasticsearchUrl.isBlank()) {
            log.info("Elasticsearch not configured - skipping indexing");
            return;
        }

        Instant now = Instant.now();
        Instant since = now.minus(30, ChronoUnit.MINUTES);
        int indexed = 0;

        String[] collections = {"requests", "workflows", "workflow_executions", "audit_logs", "documents"};

        for (String collection : collections) {
            try {
                Query query = Query.query(
                        Criteria.where("updatedAt").gte(since).orOperator(
                                Criteria.where("createdAt").gte(since))
                );

                List<Map> documents = mongoTemplate.find(query, Map.class, collection);
                if (documents.isEmpty()) {
                    continue;
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                for (Map doc : documents) {
                    try {
                        String docId = doc.get("_id") != null ? doc.get("_id").toString() : null;
                        if (docId == null) continue;

                        String indexUrl = elasticsearchUrl + "/" + collection + "/_doc/" + docId;
                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(doc, headers);
                        restTemplate.put(indexUrl, entity);
                        indexed++;

                    } catch (Exception e) {
                        log.warn("Failed to index {} document {}: {}", collection,
                                doc.get("_id"), e.getMessage());
                    }
                }

                log.info("Indexed {} documents into ES collection '{}'", documents.size(), collection);

            } catch (Exception e) {
                log.error("Failed to process ES indexing for collection '{}': {}", collection, e.getMessage());
            }
        }

        log.info("Elasticsearch indexing complete: {} documents indexed", indexed);
    }
}
