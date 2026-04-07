package org.hylee.phms.server.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 饮食记录持久化对象（可关联推荐食谱、软删除状态等）。
 */
public class MealRecordDO {

    private Long recordId;
    private Long userId;
    private Long recipeId;
    private String foodName;
    private Integer mealType;
    private LocalDateTime diningTime;
    private BigDecimal intakeAmount;
    private BigDecimal caloriesPer100g;
    private BigDecimal estimatedCalories;
    private String remark;
    private Integer recordStatus;
    private LocalDateTime createdTime;
    private LocalDateTime lastChangeTime;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public Integer getMealType() {
        return mealType;
    }

    public void setMealType(Integer mealType) {
        this.mealType = mealType;
    }

    public LocalDateTime getDiningTime() {
        return diningTime;
    }

    public void setDiningTime(LocalDateTime diningTime) {
        this.diningTime = diningTime;
    }

    public BigDecimal getIntakeAmount() {
        return intakeAmount;
    }

    public void setIntakeAmount(BigDecimal intakeAmount) {
        this.intakeAmount = intakeAmount;
    }

    public BigDecimal getCaloriesPer100g() {
        return caloriesPer100g;
    }

    public void setCaloriesPer100g(BigDecimal caloriesPer100g) {
        this.caloriesPer100g = caloriesPer100g;
    }

    public BigDecimal getEstimatedCalories() {
        return estimatedCalories;
    }

    public void setEstimatedCalories(BigDecimal estimatedCalories) {
        this.estimatedCalories = estimatedCalories;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(Integer recordStatus) {
        this.recordStatus = recordStatus;
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
