package org.hylee.phms.server.persistence;

import java.math.BigDecimal;

/**
 * 食谱评分查询/展示用持久化对象（聚合均值、次数与当前用户打分）。
 */
public class MealRecommendationRatingDO {

    private Long recipeId;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer userScore;

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
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
