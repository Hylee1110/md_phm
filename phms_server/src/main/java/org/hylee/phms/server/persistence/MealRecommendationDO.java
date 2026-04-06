package org.hylee.phms.server.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MealRecommendationDO {

    private Long recipeId;
    private Long createdBy;
    private Integer adminRecommend;
    private Integer recipeStatus;
    private Integer mealType;
    private String foodName;
    private BigDecimal portion;
    private String unit;
    private Integer calories;
    private String imageUrl;
    private String description;
    private LocalDateTime createdTime;
    private LocalDateTime lastChangeTime;
    private String creatorAccount;
    private String creatorNickname;
    private String creatorRealname;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer userScore;

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getAdminRecommend() {
        return adminRecommend;
    }

    public void setAdminRecommend(Integer adminRecommend) {
        this.adminRecommend = adminRecommend;
    }

    public Integer getRecipeStatus() {
        return recipeStatus;
    }

    public void setRecipeStatus(Integer recipeStatus) {
        this.recipeStatus = recipeStatus;
    }

    public Integer getMealType() {
        return mealType;
    }

    public void setMealType(Integer mealType) {
        this.mealType = mealType;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public BigDecimal getPortion() {
        return portion;
    }

    public void setPortion(BigDecimal portion) {
        this.portion = portion;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCreatorNickname() {
        return creatorNickname;
    }

    public void setCreatorNickname(String creatorNickname) {
        this.creatorNickname = creatorNickname;
    }

    public String getCreatorRealname() {
        return creatorRealname;
    }

    public void setCreatorRealname(String creatorRealname) {
        this.creatorRealname = creatorRealname;
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
}
