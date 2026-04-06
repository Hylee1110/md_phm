package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.SportDictionaryOptionDO;

import java.util.List;

public interface SportDictionaryMapper {

    List<SportDictionaryOptionDO> selectAudiences();

    List<SportDictionaryOptionDO> selectEquipments();

    List<SportDictionaryOptionDO> selectBenefits();

    Long countAudienceIds(@Param("ids") List<Long> ids);

    Long countEquipmentIds(@Param("ids") List<Long> ids);

    Long countBenefitIds(@Param("ids") List<Long> ids);

    SportDictionaryOptionDO selectAudienceByName(@Param("name") String name);

    SportDictionaryOptionDO selectEquipmentByName(@Param("name") String name);

    SportDictionaryOptionDO selectBenefitByName(@Param("name") String name);

    int insertAudience(@Param("name") String name);

    int insertEquipment(@Param("name") String name);

    int insertBenefit(@Param("name") String name);
}
