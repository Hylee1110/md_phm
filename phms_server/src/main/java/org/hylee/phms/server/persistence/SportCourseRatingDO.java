package org.hylee.phms.server.persistence;

import java.math.BigDecimal;

/**
 * 运动课程评分查询/展示用持久化对象。
 */
public class SportCourseRatingDO {

    private Long courseId;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer userScore;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
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
