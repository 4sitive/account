package com.f4sitive.account.repository;

import com.f4sitive.account.config.DataConfig;
import com.f4sitive.account.config.JpaConfig;
import com.f4sitive.account.config.SqlConfig;
import com.f4sitive.account.config.TestConfig;
import com.f4sitive.account.util.ThreadUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

//@Import({SqlConfig.class})//, TestConfig.class
//@JsonTest
//@ActiveProfiles("localhost")

@DataJpaTest
@Import({DataConfig.class, JpaConfig.class})
class TestRepositoryTest {
//    @MockBean
//    private ObjectMapper objectMapper;
    @Autowired
    private TestRepository testRepository;
    @Test
    void test() throws InterruptedException {
        ThreadUtils.get(() -> {
            com.f4sitive.account.entity.Test test = new com.f4sitive.account.entity.Test();
//            test.setId(UUID.randomUUID().toString());
            return testRepository.save(test);
        }, 10, 2);
    }

}