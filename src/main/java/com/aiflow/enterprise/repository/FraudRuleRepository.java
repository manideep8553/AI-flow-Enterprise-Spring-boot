package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.FraudRule;
import com.aiflow.enterprise.enums.FraudCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudRuleRepository extends MongoRepository<FraudRule, String> {

    Optional<FraudRule> findByRuleName(String ruleName);

    List<FraudRule> findByEnabledTrue();

    List<FraudRule> findByCategory(FraudCategory category);

    List<FraudRule> findByEnabledTrueOrderByPriorityDesc();

    boolean existsByRuleName(String ruleName);
}
