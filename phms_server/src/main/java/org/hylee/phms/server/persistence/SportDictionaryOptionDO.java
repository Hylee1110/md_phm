package org.hylee.phms.server.persistence;

/**
 * 运动课程字典选项持久化对象（受众、器械、收益等共用结构）。
 */
public class SportDictionaryOptionDO {

    private Long id;
    private String name;

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
}
