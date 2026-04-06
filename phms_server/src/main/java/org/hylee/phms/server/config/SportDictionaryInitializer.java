package org.hylee.phms.server.config;

import org.hylee.phms.server.mapper.SportDictionaryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SportDictionaryInitializer implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SportDictionaryInitializer.class);

    private final SportDictionaryMapper sportDictionaryMapper;

    public SportDictionaryInitializer(SportDictionaryMapper sportDictionaryMapper) {
        this.sportDictionaryMapper = sportDictionaryMapper;
    }

    @Override
    public void run(String... args) {
        try {
            List<String> audiences = List.of("全部人群", "青少年", "成年人", "老年人");
            List<String> equipments = List.of("无需器械", "跳绳", "哑铃", "瑜伽垫");
            List<String> benefits = List.of("燃脂", "增肌", "缓解压力", "提升柔韧性");

            for (String name : audiences) {
                sportDictionaryMapper.insertAudience(name);
            }
            for (String name : equipments) {
                sportDictionaryMapper.insertEquipment(name);
            }
            for (String name : benefits) {
                sportDictionaryMapper.insertBenefit(name);
            }
        } catch (Exception ex) {
            LOG.warn("Skip sport dictionary init: {}", ex.getMessage());
        }
    }
}
