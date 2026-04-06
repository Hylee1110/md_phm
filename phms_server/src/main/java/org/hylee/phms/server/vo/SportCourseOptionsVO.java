package org.hylee.phms.server.vo;

import java.util.List;

public record SportCourseOptionsVO(
        List<SportDictionaryOptionVO> audiences,
        List<SportDictionaryOptionVO> equipments,
        List<SportDictionaryOptionVO> benefits
) {
}
