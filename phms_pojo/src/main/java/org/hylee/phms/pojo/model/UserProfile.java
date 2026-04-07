package org.hylee.phms.pojo.model;

import java.util.List;

/**
 * 用户基础档案模型。
 * <p>
 * 该模型用于跨模块共享（后端服务、统计汇总等），避免直接依赖持久化 DO。
 *
 * @param userId   用户 ID
 * @param name     姓名/昵称（用于展示）
 * @param age      年龄（可为空）
 * @param gender   性别（可为空）
 * @param heightCm 身高（厘米，可为空）
 * @param weightKg 体重（千克，可为空）
 * @param tags     标签（如“偏瘦/超重/高血压风险”等，可为空或空列表）
 */
public record UserProfile(
        Long userId,
        String name,
        Integer age,
        String gender,
        Double heightCm,
        Double weightKg,
        List<String> tags
) {
}
