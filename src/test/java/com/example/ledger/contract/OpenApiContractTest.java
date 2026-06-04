package com.example.ledger.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private OpenApiInteractionValidator validator;

    private static final String ACCOUNT_ID = "contract-test-acc";

    @BeforeEach
    void setUp() throws Exception {
        java.net.URL specUrl = getClass().getClassLoader().getResource("api/ledger-api.yaml");
        assertThat(specUrl).as("ledger-api.yaml must be on classpath").isNotNull();
        validator = OpenApiInteractionValidator
                .createForSpecificationUrl(specUrl.toString())
                .build();
    }

    @Test
    void depositResponseConformsToContract() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Transaction-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("accountId", ACCOUNT_ID, "amount", 100.00, "description", "Test"))))
                .andReturn();

        assertResponseConformsToSpec("POST", "/api/v1/transactions/deposit", result);
    }

    @Test
    void withdrawResponseConformsToContract() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Transaction-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("accountId", ACCOUNT_ID, "amount", 500.00))))
                .andReturn();

        MvcResult result = mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .header("Transaction-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("accountId", ACCOUNT_ID, "amount", 100.00, "description", "Test"))))
                .andReturn();

        assertResponseConformsToSpec("POST", "/api/v1/transactions/withdraw", result);
    }

    @Test
    void balanceResponseConformsToContract() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/accounts/" + ACCOUNT_ID + "/balance")).andReturn();
        assertResponseConformsToSpec("GET", "/api/v1/accounts/" + ACCOUNT_ID + "/balance", result);
    }

    @Test
    void transactionResponseConformsToContract() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/accounts/" + ACCOUNT_ID + "/transactions")).andReturn();
        assertResponseConformsToSpec("GET", "/api/v1/accounts/" + ACCOUNT_ID + "/transactions", result);
    }

    private void assertResponseConformsToSpec(String method, String path, MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();

        SimpleResponse simpleResponse = SimpleResponse.Builder
                .status(response.getStatus())
                .withContentType(response.getContentType())
                .withBody(response.getContentAsString())
                .build();

        ValidationReport report = validator.validateResponse(path,
                com.atlassian.oai.validator.model.Request.Method.valueOf(method), simpleResponse);

        assertThat(report.hasErrors())
                .as("Contract violations for %s %s: %s", method, path,
                        report.getMessages().stream()
                                .map(ValidationReport.Message::getMessage)
                                .toList())
                .isFalse();
    }
}
