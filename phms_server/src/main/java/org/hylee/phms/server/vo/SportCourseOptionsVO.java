package org.hylee.phms.server.vo;

import java.util.List;

/**
 * 管理端课程表单所需字典下拉数据（受众、器械、收益）。
 */
public record SportCourseOptionsVO(
        List<SportDictionaryOptionVO> audiences,
        List<SportDictionaryOptionVO> equipments,
        List<SportDictionaryOptionVO> benefits
) {
}
