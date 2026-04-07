package org.hylee.phms.server.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运动课程持久化对象（封面、难度、关联字典 ID 列表等）。
 */
public class SportCourseDO {

    private Long id;
    private String name;
    private String coverUrl;
    private String summary;
    private String description;
    private Integer recommendDurationMin;
    private Integer caloriesPerHour;
    private Integer recommendFrequencyPerWeek;
    private String level;
    private String status;
    private Integer isDeleted;
    private Integer sortWeight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer userScore;
    private String audiencesCsv;
    private String equipmentsCsv;
    private String benefitsCsv;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRecommendDurationMin() {
        return recommendDurationMin;
    }

    public void setRecommendDurationMin(Integer recommendDurationMin) {
        this.recommendDurationMin = recommendDurationMin;
    }

    public Integer getCaloriesPerHour() {
        return caloriesPerHour;
    }

    public void setCaloriesPerHour(Integer caloriesPerHour) {
        this.caloriesPerHour = caloriesPerHour;
    }

    public Integer getRecommendFrequencyPerWeek() {
        return recommendFrequencyPerWeek;
    }

    public void setRecommendFrequencyPerWeek(Integer recommendFrequencyPerWeek) {
        this.recommendFrequencyPerWeek = recommendFrequencyPerWeek;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Integer getSortWeight() {
        return sortWeight;
    }

    public void setSortWeight(Integer sortWeight) {
        this.sortWeight = sortWeight;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public void setRatingAvg(BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Integer getUserScore() {
        return userScore;
    }

    public void setUserScore(Integer userScore) {
        this.userScore = userScore;
    }

    public String getAudiencesCsv() {
        return audiencesCsv;
    }

    public void setAudiencesCsv(String audiencesCsv) {
        this.audiencesCsv = audiencesCsv;
    }

    public String getEquipmentsCsv() {
        return equipmentsCsv;
    }

    public void setEquipmentsCsv(String equipmentsCsv) {
        this.equipmentsCsv = equipmentsCsv;
    }

    public String getBenefitsCsv() {
        return benefitsCsv;
    }

    public void setBenefitsCsv(String benefitsCsv) {
        this.benefitsCsv = benefitsCsv;
    }
}
