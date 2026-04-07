package org.hylee.phms.server.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健康打卡记录持久化对象（关联用户目标与目标模板）。
 */
public class HealthRecordDO {

    private Long recordId;
    private Long userGoalId;
    private Long userId;
    private Long goalId;
    private BigDecimal recordValue;
    private String recordText;
    private LocalDateTime recordTime;
    private Integer recordSource;
    private Integer evaluationResult;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime lastChangeTime;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

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

    public BigDecimal getRecordValue() {
        return recordValue;
    }

    public void setRecordValue(BigDecimal recordValue) {
        this.recordValue = recordValue;
    }

    public String getRecordText() {
        return recordText;
    }

    public void setRecordText(String recordText) {
        this.recordText = recordText;
    }

    public LocalDateTime getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(LocalDateTime recordTime) {
        this.recordTime = recordTime;
    }

    public Integer getRecordSource() {
        return recordSource;
    }

    public void setRecordSource(Integer recordSource) {
        this.recordSource = recordSource;
    }

    public Integer getEvaluationResult() {
        return evaluationResult;
    }

    public void setEvaluationResult(Integer evaluationResult) {
        this.evaluationResult = evaluationResult;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
}
