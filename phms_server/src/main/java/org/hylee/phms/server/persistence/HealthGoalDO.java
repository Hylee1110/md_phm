package org.hylee.phms.server.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健康目标模板持久化对象（可被用户选择）。
 */
public class HealthGoalDO {

    private Long goalId;
    private String goalCode;
    private String goalName;
    private String goalDescription;
    private Integer metricType;
    private String unit;
    private BigDecimal defaultTargetMin;
    private BigDecimal defaultTargetMax;
    private String defaultTargetText;
    private Integer sortNo;
    private Integer goalStatus;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime lastChangeTime;
    private String creatorAccount;
    private String creatorName;
    private Long userGoalId;
    private Integer userGoalStatus;
    private BigDecimal targetMin;
    private BigDecimal targetMax;
    private String targetText;

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
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

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Integer getGoalStatus() {
        return goalStatus;
    }

    public void setGoalStatus(Integer goalStatus) {
        this.goalStatus = goalStatus;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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

    public String getCreatorAccount() {
        return creatorAccount;
    }

    public void setCreatorAccount(String creatorAccount) {
        this.creatorAccount = creatorAccount;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Long getUserGoalId() {
        return userGoalId;
    }

    public void setUserGoalId(Long userGoalId) {
        this.userGoalId = userGoalId;
    }

    public Integer getUserGoalStatus() {
        return userGoalStatus;
    }

    public void setUserGoalStatus(Integer userGoalStatus) {
        this.userGoalStatus = userGoalStatus;
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
}
