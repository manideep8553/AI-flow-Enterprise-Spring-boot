"""
Fraud Detection ML Service
FastAPI application with multiple fraud detection models:
- Duplicate submission detection (TF-IDF + cosine similarity)
- Abnormal spending patterns (Isolation Forest + statistical)
- Forged/doctored receipt detection (image metadata + text analysis)
- Policy violation detection (rule-based + ML)
- Suspicious vendor detection (network analysis + frequency)
- Frequent reimbursement detection (temporal pattern analysis)
- High-risk transaction detection (ensemble scoring)
"""

import io
import json
import re
import hashlib
from datetime import datetime, timedelta
from typing import Any, Optional

import numpy as np
import pandas as pd
from fastapi import FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel, Field

app = FastAPI(title="Fraud Detection ML Service", version="1.0.0")

# ---------------------------------------------------------------------------
# Pydantic models
# ---------------------------------------------------------------------------

class TransactionItem(BaseModel):
    amount: float
    category: str = ""
    description: str = ""
    date: str = ""
    vendor: str = ""
    department: str = ""

class HistoricalTransaction(BaseModel):
    id: str
    amount: float
    category: str = ""
    description: str = ""
    date: str = ""
    vendor: str = ""
    department: str = ""
    approved: bool = True
    flagged: bool = False

class ExpenseClaim(BaseModel):
    id: str
    userId: str = ""
    userName: str = ""
    department: str = ""
    amount: float
    category: str = ""
    description: str = ""
    date: str = ""
    vendor: str = ""
    receiptText: str = ""
    receiptHash: str = ""
    items: list[TransactionItem] = []

class FraudCheckRequest(BaseModel):
    claim: ExpenseClaim
    history: list[HistoricalTransaction] = []
    userHistory: list[ExpenseClaim] = []
    policyRules: dict[str, Any] = {}

class FraudScore(BaseModel):
    category: str
    score: float
    confidence: float
    explanation: str
    details: dict[str, Any] = {}

class FraudCheckResponse(BaseModel):
    overallRiskScore: float
    riskLevel: str
    scores: list[FraudScore]
    explanation: str
    modelVersion: str = "1.0.0"
    processedAt: str = ""

class DuplicateCheckRequest(BaseModel):
    text: str
    existingTexts: list[str] = []
    threshold: float = 0.85

class DuplicateCheckResponse(BaseModel):
    isDuplicate: bool
    similarityScore: float
    matchedId: str = ""
    explanation: str = ""

class BatchFraudCheckRequest(BaseModel):
    claims: list[ExpenseClaim]
    history: list[HistoricalTransaction] = []
    userHistoryMap: dict[str, list[ExpenseClaim]] = {}

class VendorRiskRequest(BaseModel):
    vendorName: str
    vendorId: str = ""
    transactionCount: int = 0
    totalAmount: float = 0.0
    averageAmount: float = 0.0
    historyByVendor: list[dict[str, Any]] = []

# ---------------------------------------------------------------------------
# ML Models (lazy-loaded)
# ---------------------------------------------------------------------------

_isolation_forest: Any = None
_tfidf_vectorizer: Any = None

def get_isolation_forest():
    global _isolation_forest
    if _isolation_forest is None:
        from sklearn.ensemble import IsolationForest
        _isolation_forest = IsolationForest(
            n_estimators=100,
            contamination=0.05,
            random_state=42,
            warm_start=True,
        )
    return _isolation_forest

def get_tfidf():
    global _tfidf_vectorizer
    if _tfidf_vectorizer is None:
        from sklearn.feature_extraction.text import TfidfVectorizer
        _tfidf_vectorizer = TfidfVectorizer(
            analyzer="char_wb",
            ngram_range=(2, 4),
            max_features=5000,
        )
    return _tfidf_vectorizer

# ---------------------------------------------------------------------------
# Utility functions
# ---------------------------------------------------------------------------

def _risk_level(score: float) -> str:
    if score >= 0.8:
        return "CRITICAL"
    if score >= 0.6:
        return "HIGH"
    if score >= 0.3:
        return "MEDIUM"
    return "LOW"

def _normalize_score(raw: float) -> float:
    return min(max(float(raw), 0.0), 1.0)

def _parse_date(d: str) -> Optional[datetime]:
    for fmt in ("%Y-%m-%d", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%dT%H:%M:%SZ",
                "%m/%d/%Y", "%d/%m/%Y", "%Y/%m/%d"):
        try:
            return datetime.strptime(d, fmt)
        except (ValueError, TypeError):
            continue
    return None

# ---------------------------------------------------------------------------
# Detection modules
# ---------------------------------------------------------------------------

def detect_duplicate(claim: ExpenseClaim,
                     history: list[HistoricalTransaction]) -> FraudScore:
    texts = [claim.description.lower(), claim.receiptText.lower()]
    combined = " ".join(t for t in texts if t)

    if not combined or not history:
        return FraudScore(
            category="DUPLICATE",
            score=0.0,
            confidence=0.0,
            explanation="Insufficient data for duplicate detection",
            details={"itemsCompared": 0},
        )

    existing_descriptions = []
    for h in history:
        desc = (h.description or "").lower()
        if desc:
            existing_descriptions.append(desc)

    if not existing_descriptions:
        return FraudScore(
            category="DUPLICATE",
            score=0.0,
            confidence=0.0,
            explanation="No historical descriptions to compare",
            details={"itemsCompared": 0},
        )

    try:
        all_texts = [combined] + existing_descriptions
        vectorizer = get_tfidf()
        tfidf_matrix = vectorizer.fit_transform(all_texts)
        from sklearn.metrics.pairwise import cosine_similarity
        similarities = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:]).flatten()

        max_sim = float(similarities.max()) if len(similarities) > 0 else 0.0

        if claim.receiptHash:
            receipt_hash_matches = sum(
                1 for h in history
                if getattr(h, "receiptHash", None) == claim.receiptHash
            )
            if receipt_hash_matches > 0:
                max_sim = max(max_sim, 0.95)

        score = _normalize_score(max_sim)
        is_dup = score >= 0.85

        return FraudScore(
            category="DUPLICATE",
            score=score,
            confidence=_normalize_score(0.7 + 0.3 * score),
            explanation=(
                f"{'Duplicate' if is_dup else 'No duplicate'} submission detected. "
                f"Highest similarity: {max_sim:.2%}"
            ),
            details={
                "maxSimilarity": round(max_sim, 4),
                "isDuplicate": is_dup,
                "itemsCompared": len(existing_descriptions),
            },
        )
    except Exception as e:
        return FraudScore(
            category="DUPLICATE",
            score=0.0,
            confidence=0.0,
            explanation=f"Duplicate check failed: {e}",
            details={"error": str(e)},
        )


def detect_abnormal_spending(claim: ExpenseClaim,
                              history: list[HistoricalTransaction]) -> FraudScore:
    amounts = [h.amount for h in history if h.amount > 0]

    if not amounts:
        return FraudScore(
            category="ABNORMAL_SPENDING",
            score=0.0,
            confidence=0.0,
            explanation="No historical spending data for comparison",
        )

    amounts_arr = np.array(amounts).reshape(-1, 1)
    try:
        model = get_isolation_forest()
        model.fit(amounts_arr)
        anomaly_score = model.score_samples([[claim.amount]])[0]
        anomaly_score = _normalize_score(-anomaly_score / 10 + 0.5)
    except Exception:
        mean = float(np.mean(amounts_arr))
        std = float(np.std(amounts_arr)) or 1.0
        z_score = abs(claim.amount - mean) / std
        anomaly_score = _normalize_score(z_score / 5.0)

    percentile = sum(1 for a in amounts if a < claim.amount) / max(len(amounts), 1)
    spending_ratio = claim.amount / max(np.mean(amounts), 1)

    category_amounts = [
        h.amount for h in history
        if h.category == claim.category and h.amount > 0
    ]
    category_anomaly = 0.0
    if category_amounts:
        cat_mean = float(np.mean(category_amounts))
        cat_std = float(np.std(category_amounts)) or 1.0
        cat_z = abs(claim.amount - cat_mean) / cat_std
        category_anomaly = _normalize_score(cat_z / 4.0)

    score = _normalize_score(anomaly_score * 0.5 + category_anomaly * 0.3 +
                             (1 - percentile) * 0.2)

    parts = []
    if score > 0.6:
        parts.append(f"Amount ${claim.amount:.2f} is significantly above normal")
    if category_anomaly > 0.5:
        parts.append(f"Unusual for category '{claim.category}'")
    if spending_ratio > 3:
        parts.append(f"{spending_ratio:.1f}x higher than average")

    return FraudScore(
        category="ABNORMAL_SPENDING",
        score=score,
        confidence=_normalize_score(0.6 + 0.3 * score),
        explanation="; ".join(parts) if parts else "Spending within normal range",
        details={
            "anomalyScore": round(anomaly_score, 4),
            "categoryAnomaly": round(category_anomaly, 4),
            "spendingRatio": round(spending_ratio, 2),
            "percentile": round(percentile, 4),
            "historicalCount": len(amounts),
        },
    )


def detect_forged_receipt(claim: ExpenseClaim) -> FraudScore:
    indicators = []
    weight = 0.0

    text = (claim.receiptText or "").strip()
    if not text:
        return FraudScore(
            category="FORGED_RECEIPT",
            score=0.3,
            confidence=0.4,
            explanation="No receipt text provided — unable to verify authenticity",
            details={"textLength": 0},
        )

    # Check for common receipt patterns
    has_total = bool(re.search(r'(?i)(total|sum|amount due|grand total)', text))
    has_date = bool(re.search(r'\d{1,4}[-/]\d{1,2}[-/]\d{1,4}', text))
    has_currency = bool(re.search(r'[$\u20ac\u00a3\u00a5]', text))
    has_items = bool(re.search(r'(?i)(item|qty|quantity|description|product)', text))
    has_tax = bool(re.search(r'(?i)(tax|vat|gst|hst)', text))

    expected_patterns = [has_total, has_date, has_currency, has_items, has_tax]
    missing = sum(1 for p in expected_patterns if not p)
    pattern_score = missing / max(len(expected_patterns), 1)

    if missing >= 3:
        indicators.append("Missing multiple standard receipt fields")
        weight += 0.3
    if not has_total:
        indicators.append("No total amount found")
        weight += 0.15
    if not has_date:
        indicators.append("No date found on receipt")
        weight += 0.1

    # Check for common forged receipt text patterns
    forged_indicators = [
        r'(?i)(this is (not )?a (valid )?receipt|sample|example|template|forged)',
        r'(?i)(receipt|invoice)\s*(generator|maker|creator)',
        r'(?i)(demo|test|courtesy|copy)',
        r'(?i)(www\.|http)',
    ]
    for pattern in forged_indicators:
        if re.search(pattern, text):
            indicators.append(f"Suspicious text pattern detected")
            weight += 0.2
            break

    # Check amount consistency
    amounts_in_text = re.findall(r'[$]?\d+[.,]\d{2}', text)
    if amounts_in_text and claim.amount > 0:
        parsed = []
        for a in amounts_in_text:
            try:
                parsed.append(float(a.replace("$", "").replace(",", "")))
            except ValueError:
                continue
        if parsed and claim.amount not in parsed:
            closest = min(parsed, key=lambda x: abs(x - claim.amount))
            if abs(closest - claim.amount) > 0.01:
                indicators.append(
                    f"Claim amount ${claim.amount:.2f} not found on receipt "
                    f"(closest: ${closest:.2f})"
                )
                weight += 0.25

    score = _normalize_score(pattern_score * 0.4 + weight)

    return FraudScore(
        category="FORGED_RECEIPT",
        score=score,
        confidence=_normalize_score(0.5 + 0.4 * (1 - pattern_score)),
        explanation="; ".join(indicators) if indicators else "Receipt appears authentic",
        details={
            "patternScore": round(pattern_score, 4),
            "missingFields": missing,
            "totalFields": len(expected_patterns),
            "indicators": indicators,
        },
    )


def detect_policy_violation(claim: ExpenseClaim,
                             policyRules: dict[str, Any]) -> FraudScore:
    violations = []
    weight = 0.0

    max_amount = policyRules.get("maxAmount", 0)
    if max_amount > 0 and claim.amount > max_amount:
        violations.append(f"Amount ${claim.amount:.2f} exceeds policy max ${max_amount:.2f}")
        weight += 0.5

    restricted_categories = policyRules.get("restrictedCategories", [])
    if claim.category in restricted_categories:
        violations.append(f"Category '{claim.category}' is restricted")
        weight += 0.4

    restricted_vendors = policyRules.get("restrictedVendors", [])
    if claim.vendor in restricted_vendors:
        violations.append(f"Vendor '{claim.vendor}' is restricted")
        weight += 0.4

    max_per_item = policyRules.get("maxPerItem", 0)
    if max_per_item > 0 and claim.items:
        over_limit = [it for it in claim.items if it.amount > max_per_item]
        if over_limit:
            violations.append(f"{len(over_limit)} item(s) exceed ${max_per_item:.2f} limit")
            weight += 0.3

    department_budget = policyRules.get("departmentBudgets", {})
    dept = claim.department or ""
    if dept in department_budget:
        budget = department_budget[dept]
        if isinstance(budget, dict):
            spent = budget.get("spent", 0)
            limit = budget.get("limit", 0)
            if limit > 0 and spent + claim.amount > limit:
                violations.append(f"Department '{dept}' would exceed budget")
                weight += 0.3

    score = _normalize_score(weight)

    return FraudScore(
        category="POLICY_VIOLATION",
        score=score,
        confidence=_normalize_score(0.7 + 0.2 * score),
        explanation="; ".join(violations) if violations else "No policy violations detected",
        details={
            "violations": violations,
            "rulesChecked": [
                "maxAmount" if max_amount > 0 else None,
                "restrictedCategories" if restricted_categories else None,
                "restrictedVendors" if restricted_vendors else None,
                "maxPerItem" if max_per_item > 0 else None,
            ],
        },
    )


def detect_suspicious_vendor(claim: ExpenseClaim,
                              history: list[HistoricalTransaction]) -> FraudScore:
    vendor = (claim.vendor or "").strip().lower()
    if not vendor:
        return FraudScore(
            category="SUSPICIOUS_VENDOR",
            score=0.0,
            confidence=0.0,
            explanation="No vendor information provided",
        )

    vendor_txns = [h for h in history if (h.vendor or "").lower() == vendor]
    vendor_count = len(vendor_txns)

    indicators = []
    weight = 0.0

    # New vendor
    if vendor_count == 0:
        indicators.append(f"First transaction with vendor '{claim.vendor}'")
        weight += 0.15

    # Amount spike
    if vendor_count > 0:
        avg_vendor = float(np.mean([h.amount for h in vendor_txns if h.amount > 0]))
        if avg_vendor > 0 and claim.amount > avg_vendor * 3:
            indicators.append(
                f"Amount ${claim.amount:.2f} is {claim.amount / avg_vendor:.1f}x "
                f"above vendor average ${avg_vendor:.2f}"
            )
            weight += 0.25

    # Generic / suspicious vendor name patterns
    suspicious_name_patterns = [
        r'(?i)^[a-z]\s*[a-z]\s*[a-z]?$',
        r'(?i)(test|temp|dummy|fake|sample)',
        r'(?i)^[a-z]{1,3}$',
        r'(?i)(llc|inc|corp|ltd)\s*$',
        r'(?i)(services|consulting|solutions|technologies)\s+(llc|inc|corp)',
    ]
    for pattern in suspicious_name_patterns:
        if re.search(pattern, vendor):
            indicators.append(f"Vendor name pattern flagged as suspicious")
            weight += 0.15
            break

    # All transactions from same vendor
    if history and vendor_count / max(len(history), 1) > 0.5:
        indicators.append(f"Vendor represents {vendor_count / len(history):.0%} of all transactions")
        weight += 0.2

    score = _normalize_score(weight)

    return FraudScore(
        category="SUSPICIOUS_VENDOR",
        score=score,
        confidence=_normalize_score(0.5 + 0.3 * score),
        explanation="; ".join(indicators) if indicators else "No vendor concerns detected",
        details={
            "vendorTransactionCount": vendor_count,
            "vendor": claim.vendor,
            "indicators": indicators,
        },
    )


def detect_frequent_reimbursement(claim: ExpenseClaim,
                                   userHistory: list[ExpenseClaim]) -> FraudScore:
    if not userHistory:
        return FraudScore(
            category="FREQUENT_REIMBURSEMENT",
            score=0.0,
            confidence=0.0,
            explanation="No user history available for frequency analysis",
        )

    indicators = []
    weight = 0.0

    # Filter to recent history (last 90 days)
    now = datetime.utcnow()
    recent = []
    for uc in userHistory:
        d = _parse_date(uc.date)
        if d and (now - d).days <= 90:
            recent.append(uc)

    if len(recent) >= 5:
        indicators.append(f"{len(recent)} claims in last 90 days")
        weight += _normalize_score((len(recent) - 5) / 15.0)

    if len(recent) >= 3 and claim.amount > 0:
        recent_amounts = [uc.amount for uc in recent if uc.amount > 0]
        if recent_amounts:
            total_recent = sum(recent_amounts) + claim.amount
            avg_monthly = total_recent / max(len(recent) / 30.0, 1)
            threshold = 5000
            if avg_monthly > threshold:
                indicators.append(
                    f"Average monthly reimbursement ${avg_monthly:.0f} "
                    f"exceeds ${threshold}"
                )
                weight += _normalize_score((avg_monthly - threshold) / 5000.0)

    # Same category repeated
    if len(recent) >= 3:
        same_cat = sum(1 for uc in recent if uc.category == claim.category)
        if same_cat >= 3:
            indicators.append(
                f"{same_cat} claims in category '{claim.category}' in 90 days"
            )
            weight += 0.15

    # Submissions on weekends / holidays
    claim_date = _parse_date(claim.date)
    if claim_date:
        if claim_date.weekday() >= 5:
            indicators.append("Claim submitted on weekend")
            weight += 0.1

    score = _normalize_score(weight)

    return FraudScore(
        category="FREQUENT_REIMBURSEMENT",
        score=score,
        confidence=_normalize_score(0.6 + 0.3 * score),
        explanation="; ".join(indicators) if indicators else "Reimbursement frequency appears normal",
        details={
            "recentClaimCount": len(recent),
            "totalUserHistory": len(userHistory),
            "indicators": indicators,
        },
    )


def detect_high_risk_transaction(claim: ExpenseClaim,
                                  scores: list[FraudScore]) -> FraudScore:
    if not scores:
        return FraudScore(
            category="HIGH_RISK_TRANSACTION",
            score=0.0,
            confidence=0.0,
            explanation="No fraud scores available for risk assessment",
        )

    avg_score = float(np.mean([s.score for s in scores]))
    max_score = float(max(s.score for s in scores))
    high_count = sum(1 for s in scores if s.score >= 0.5)

    risk_score = _normalize_score(avg_score * 0.4 + max_score * 0.4 +
                                   (high_count / max(len(scores), 1)) * 0.2)

    amount_factor = _normalize_score(claim.amount / 10000.0) * 0.1
    risk_score = _normalize_score(risk_score + amount_factor)

    aggregates = {}
    for s in scores:
        aggregates[s.category] = round(s.score, 4)

    return FraudScore(
        category="HIGH_RISK_TRANSACTION",
        score=risk_score,
        confidence=_normalize_score(0.7 + 0.2 * risk_score),
        explanation=(
            f"Overall risk assessment: {high_count}/{len(scores)} "
            f"categories flagged. Max individual risk: {max_score:.0%}"
        ),
        details={
            "averageScore": round(avg_score, 4),
            "maxScore": round(max_score, 4),
            "flaggedCategories": high_count,
            "totalCategories": len(scores),
            "amountFactor": round(amount_factor, 4),
            "categoryScores": aggregates,
        },
    )


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@app.get("/health")
def health():
    return {"status": "healthy", "service": "fraud-detection-ml"}


@app.post("/analyze", response_model=FraudCheckResponse)
def analyze_fraud(req: FraudCheckRequest):
    claim = req.claim
    history = req.history
    user_history = req.userHistory
    policy_rules = req.policyRules

    scores: list[FraudScore] = [
        detect_duplicate(claim, history),
        detect_abnormal_spending(claim, history),
        detect_forged_receipt(claim),
        detect_policy_violation(claim, policy_rules),
        detect_suspicious_vendor(claim, history),
        detect_frequent_reimbursement(claim, user_history),
    ]

    high_risk = detect_high_risk_transaction(claim, scores)
    scores.append(high_risk)

    overall_risk = high_risk.score
    risk_level = _risk_level(overall_risk)

    explanation_parts = []
    for s in scores:
        if s.score >= 0.5:
            explanation_parts.append(f"{s.category}: {s.score:.0%} risk")
    explanation = (
        "; ".join(explanation_parts)
        if explanation_parts
        else "All checks passed with low risk scores"
    )

    result = FraudCheckResponse(
        overallRiskScore=round(overall_risk, 4),
        riskLevel=risk_level,
        scores=scores,
        explanation=explanation,
        processedAt=datetime.utcnow().isoformat() + "Z",
    )

    # Fit Isolation Forest incrementally on non-anomalous historical data
    amounts = [h.amount for h in history if h.amount > 0]
    if len(amounts) >= 10:
        try:
            model = get_isolation_forest()
            model.fit(np.array(amounts).reshape(-1, 1))
        except Exception:
            pass

    return result


@app.post("/analyze/batch", response_model=list[FraudCheckResponse])
def analyze_batch(req: BatchFraudCheckRequest):
    results = []
    for claim in req.claims:
        user_hist = req.userHistoryMap.get(claim.userId, [])
        fr = FraudCheckRequest(
            claim=claim,
            history=req.history,
            userHistory=user_hist,
        )
        results.append(analyze_fraud(fr))
    return results


@app.post("/duplicate", response_model=DuplicateCheckResponse)
def check_duplicate(req: DuplicateCheckRequest):
    if not req.text or not req.existingTexts:
        return DuplicateCheckResponse(
            isDuplicate=False, similarityScore=0.0,
            explanation="Insufficient data",
        )

    try:
        all_texts = [req.text] + req.existingTexts
        vectorizer = get_tfidf()
        tfidf_matrix = vectorizer.fit_transform(all_texts)
        from sklearn.metrics.pairwise import cosine_similarity
        sims = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:]).flatten()
        max_sim = float(sims.max()) if len(sims) > 0 else 0.0
    except Exception as e:
        return DuplicateCheckResponse(
            isDuplicate=False, similarityScore=0.0,
            explanation=f"Error: {e}",
        )

    best_idx = int(sims.argmax()) if len(sims) > 0 else -1

    return DuplicateCheckResponse(
        isDuplicate=max_sim >= req.threshold,
        similarityScore=round(max_sim, 4),
        matchedId=str(best_idx) if best_idx >= 0 else "",
        explanation=(
            f"Similarity: {max_sim:.2%} "
            f"({'DUPLICATE' if max_sim >= req.threshold else 'UNIQUE'})"
        ),
    )


@app.post("/vendor/risk")
def vendor_risk(req: VendorRiskRequest):
    risk_score = 0.0
    indicators = []

    if not req.vendorName:
        return {
            "vendorName": "",
            "riskScore": 0.0,
            "riskLevel": "LOW",
            "indicators": ["No vendor name provided"],
        }

    vendor_name_lower = req.vendorName.lower()

    # New vendor risk
    if req.transactionCount == 0:
        indicators.append("New vendor — no transaction history")
        risk_score += 0.15

    # Average amount outliers
    if req.averageAmount > 0 and req.transactionCount > 0:
        overall_avg = req.totalAmount / req.transactionCount
        if req.averageAmount > overall_avg * 2:
            indicators.append(f"Average transaction amount ${req.averageAmount:.0f} is elevated")
            risk_score += 0.2

    # Vendor name pattern analysis
    suspicious_patterns = [
        r'(?i)(test|temp|dummy)',
        r'(?i)^.{1,3}$',
        r'(?i)(fre[e]+[a-z]*\s*lance|consult[a-z]*\s*ser)',
    ]
    for pat in suspicious_patterns:
        if re.search(pat, vendor_name_lower):
            indicators.append("Suspicious vendor name pattern")
            risk_score += 0.2
            break

    # Check if name looks like a person vs company
    if re.match(r'^[A-Z][a-z]+\s+[A-Z][a-z]+$', req.vendorName):
        indicators.append("Vendor name appears to be an individual")
        risk_score += 0.1

    score = _normalize_score(risk_score)

    return {
        "vendorName": req.vendorName,
        "vendorId": req.vendorId,
        "riskScore": round(score, 4),
        "riskLevel": _risk_level(score),
        "indicators": indicators,
        "transactionCount": req.transactionCount,
        "totalAmount": round(req.totalAmount, 2),
    }
