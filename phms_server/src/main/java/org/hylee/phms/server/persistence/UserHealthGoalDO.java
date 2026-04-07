package org.hylee.phms.server.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户已选健康目标持久化对象（含目标区间、进度与状态）。
 */
public class UserHealthGoalDO {

    private Long userGoalId;
    private Long userId;
    private Long goalId;
    private BigDecimal targetMin;
    private BigDecimal targetMax;
    private String targetText;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer userGoalStatus;
    private LocalDateTime createdTime;
    private LocalDateTime lastChangeTime;
    private String goalCode;
    private String goalName;
    private String goalDescription;
    private Integer metricType;
    private String unit;
    private BigDecimal defaultTargetMin;
    private BigDecimal defaultTargetMax;
    private String defaultTargetText;
    private Integer goalSortNo;
    private Integer goalStatus;
    private Integer recordCount;
    private BigDecimal latestRecordValue;
    private String latestRecordText;
    private Integer latestEvaluationResult;
    private LocalDateTime latestRecordTime;

    public Long getUserGoalId() {
        return userGoalId;
    }

    public void setUserGoalId(Long userGoalId) {
        this.userGoalId = userGoalId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public BigDecimal getTargetMin() {
        return targetMin;
    }

    public void setTargetMin(BigDecimal targetMin) {
        this.targetMin = targetMin;
    }

    public BigDecimal getTargetMax() {
        return targetMax;
    }

    public void setTargetMax(BigDecimal targetMax) {
        this.targetMax = targetMax;
    }

    public String getTargetText() {
        return targetText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getUserGoalStatus() {
        return userGoalStatus;
    }

    public void setUserGoalStatus(Integer userGoalStatus) {
        this.userGoalStatus = userGoalStatus;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getLastChangeTime() {
        return lastChangeTime;
    }

    public void setLastChangeTime(LocalDateTime lastChangeTime) {
        this.lastChangeTime = lastChangeTime;
    }

    public String getGoalCode() {
        return goalCode;
    }

    public void setGoalCode(String goalCode) {
        this.goalCode = goalCode;
    }

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public String getGoalDescription() {
        return goalDescription;
    }

    public void setGoalDescription(String goalDescription) {
        this.goalDescription = goalDescription;
    }

    public Integer getMetricType() {
        return metricType;
    }

    public void setMetricType(Integer metricType) {
        this.metricType = metricType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getDefaultTargetMin() {
        return defaultTargetMin;
    }

    public void setDefaultTargetMin(BigDecimal defaultTargetMin) {
        this.defaultTargetMin = defaultTargetMin;
    }

    public BigDecimal getDefaultTargetMax() {
        return defaultTargetMax;
    }

    public void setDefaultTargetMax(BigDecimal defaultTargetMax) {
        this.defaultTargetMax = defaultTargetMax;
    }

    public String getDefaultTargetText() {
        return defaultTargetText;
    }

    public void setDefaultTargetText(String defaultTargetText) {
        this.defaultTargetText = defaultTargetText;
    }

    public Integer getGoalSortNo() {
        return goalSortNo;
    }

    public void setGoalSortNo(Integer goalSortNo) {
        this.goalSortNo = goalSortNo;
    }

    public Integer getGoalStatus() {
        return goalStatus;
    }

    public void setGoalStatus(Integer goalStatus) {
        this.goalStatus = goalStatus;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public BigDecimal getLatestRecordValue() {
        return latestRecordValue;
    }

    public void setLatestRecordValue(BigDecimal latestRecordValue) {
        this.latestRecordValue = latestRecordValue;
    }

    public String getLatestRecordText() {
        return latestRecordText;
    }

    public void setLatestRecordText(String latestRecordText) {
        this.latestRecordText = latestRecordText;
    }

    public Integer getLatestEvaluationResult() {
        return latestEvaluationResult;
    }

    public void setLatestEvaluationResult(Integer latestEvaluationResult) {
        this.latestEvaluationResult = latestEvaluationResult;
    }

    public LocalDateTime getLatestRecordTime() {
        return latestRecordTime;
    }

    public void setLatestRecordTime(LocalDateTime latestRecordTime) {
        this.latestRecordTime = latestRecordTime;
    }
}
