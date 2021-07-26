package com.f4sitive.account.config;

import com.f4sitive.account.service.UserService;
import org.apache.http.client.HttpClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers=HttpConfigTest.TestController.class)
class HttpConfigTest {
    @RestController
    class TestController {
        @GetMapping("/test")
        public String get(){
            return UUID.randomUUID().toString();
        }
    }
    @MockBean
    UserService userService;
    @MockBean
    HttpClient httpClient;
    @MockBean
    RegisteredClientRepository registeredClientRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void httpClient() throws Exception {
//        mockMvc.perform(MockMvcRequestBuilders.get("/test"))
//                .andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Hello Mock")));

        HttpConfig config = new HttpConfig();
        config.setQueryTimeout(Duration.ofMillis(-100L));
        RestTemplate restTemplate = new RestTemplateBuilder()
                .requestFactory(() ->new HttpComponentsClientHttpRequestFactory(config.httpClient()))
                .build();
//        InetAddress.getLocalHost().getHostName();
        restTemplate.getForEntity("http://localhost", String.class);
        restTemplate.getForEntity("http://kyungwookyi-ui-MacBookPro.local", String.class);
        restTemplate.getForEntity("http://google.com", String.class);
    }
}