package com.api.controller;

import com.api.dto.CreateFinancialAccountRequest;
import com.api.dto.UpdateFinancialAccountRequest;
import com.api.enums.FinancialAccountType;
import com.api.model.FinancialAccount;
import com.api.model.Transaction;
import com.api.model.User;
import com.api.model.Category;
import com.api.repository.CategoryRepository;
import com.api.repository.FinancialAccountRepository;
import com.api.repository.TransactionRepository;
import com.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinancialAccountControllerIntegrationTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FinancialAccountRepository financialAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        transactionRepository.deleteAll();
        financialAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void list_shouldReturnOnlyCurrentUsersActiveAccounts() throws Exception {
        User user = createUser("accounts-list@test.com", "List User");
        User otherUser = createUser("other-list@test.com", "Other User");

        FinancialAccount ownActive = new FinancialAccount();
        ownActive.setUser(user);
        ownActive.setName("Cash");
        ownActive.setType(FinancialAccountType.CASH);
        ownActive.setCurrency("USD");
        financialAccountRepository.save(ownActive);

        FinancialAccount otherActive = new FinancialAccount();
        otherActive.setUser(otherUser);
        otherActive.setName("Savings");
        otherActive.setType(FinancialAccountType.BANK_ACCOUNT);
        otherActive.setCurrency("EUR");
        financialAccountRepository.save(otherActive);

        FinancialAccount ownDeleted = new FinancialAccount();
        ownDeleted.setUser(user);
        ownDeleted.setName("Old Card");
        ownDeleted.setType(FinancialAccountType.CREDIT_CARD);
        ownDeleted.setCurrency("USD");
        financialAccountRepository.saveAndFlush(ownDeleted);
        financialAccountRepository.delete(ownDeleted);

        Cookie accessCookie = loginAndGetAccessCookie("accounts-list@test.com");

        mockMvc.perform(get("/api/v1/accounts").cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cash"))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void create_shouldPersistAccountAndNormalizeCurrency() throws Exception {
        createUser("account-create@test.com", "Create User");
        Cookie accessCookie = loginAndGetAccessCookie("account-create@test.com");

        CreateFinancialAccountRequest request = new CreateFinancialAccountRequest(
                "Byblos Card",
                FinancialAccountType.CREDIT_CARD,
                "usd"
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Byblos Card"))
                .andExpect(jsonPath("$.type").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.currency").value("USD"));

        FinancialAccount saved = financialAccountRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(saved.getName()).isEqualTo("Byblos Card");
        assertThat(saved.getCurrency()).isEqualTo("USD");
    }

    @Test
    void create_shouldRejectDuplicateNameIgnoringCase() throws Exception {
        User user = createUser("account-duplicate@test.com", "Duplicate User");
        FinancialAccount existing = new FinancialAccount();
        existing.setUser(user);
        existing.setName("Savings");
        existing.setType(FinancialAccountType.BANK_ACCOUNT);
        existing.setCurrency("USD");
        financialAccountRepository.save(existing);

        Cookie accessCookie = loginAndGetAccessCookie("account-duplicate@test.com");

        CreateFinancialAccountRequest request = new CreateFinancialAccountRequest(
                "savings",
                FinancialAccountType.CASH,
                "USD"
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Financial account with this name already exists"));
    }

    @Test
    void create_shouldRejectBlankNameAndInvalidCurrency() throws Exception {
        createUser("account-invalid@test.com", "Invalid User");
        Cookie accessCookie = loginAndGetAccessCookie("account-invalid@test.com");

        mockMvc.perform(post("/api/v1/accounts")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"type\":\"CASH\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Financial account name is required"));

        mockMvc.perform(post("/api/v1/accounts")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wallet\",\"type\":\"DIGITAL_WALLET\",\"currency\":\"US\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.currency").value("Currency must be a valid 3-character ISO code"));
    }

    @Test
    void create_shouldRejectInvalidEnumValue() throws Exception {
        createUser("account-invalid-enum@test.com", "Invalid Enum User");
        Cookie accessCookie = loginAndGetAccessCookie("account-invalid-enum@test.com");

        mockMvc.perform(post("/api/v1/accounts")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wallet\",\"type\":\"NOPE\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid financial account type. Allowed values: CASH, BANK_ACCOUNT, CREDIT_CARD, DEBIT_CARD, DIGITAL_WALLET, OTHER"));
    }

    @Test
    void get_shouldRejectCrossUserAccess() throws Exception {
        User user = createUser("account-owner@test.com", "Owner User");
        User otherUser = createUser("account-other@test.com", "Other User");
        FinancialAccount account = new FinancialAccount();
        account.setUser(user);
        account.setName("Main Cash");
        account.setType(FinancialAccountType.CASH);
        account.setCurrency("USD");
        account = financialAccountRepository.save(account);

        Cookie accessCookie = loginAndGetAccessCookie("account-other@test.com");

        mockMvc.perform(get("/api/v1/accounts/{accountId}", account.getId())
                        .cookie(accessCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this financial account"));
    }

    @Test
    void patch_shouldUpdateOnlyProvidedFields() throws Exception {
        User user = createUser("account-patch@test.com", "Patch User");
        FinancialAccount account = new FinancialAccount();
        account.setUser(user);
        account.setName("Old Name");
        account.setType(FinancialAccountType.BANK_ACCOUNT);
        account.setCurrency("USD");
        account = financialAccountRepository.save(account);

        Cookie accessCookie = loginAndGetAccessCookie("account-patch@test.com");

        UpdateFinancialAccountRequest request = new UpdateFinancialAccountRequest(
                "New Name",
                null,
                "eur"
        );

        mockMvc.perform(patch("/api/v1/accounts/{accountId}", account.getId())
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.type").value("BANK_ACCOUNT"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void delete_shouldSoftDeleteAccount() throws Exception {
        User user = createUser("account-delete@test.com", "Delete User");
        FinancialAccount account = new FinancialAccount();
        account.setUser(user);
        account.setName("Travel Wallet");
        account.setType(FinancialAccountType.DIGITAL_WALLET);
        account.setCurrency("USD");
        account = financialAccountRepository.save(account);

        Cookie accessCookie = loginAndGetAccessCookie("account-delete@test.com");

        mockMvc.perform(delete("/api/v1/accounts/{accountId}", account.getId())
                        .cookie(accessCookie))
                .andExpect(status().isNoContent());

        assertThat(financialAccountRepository.findById(account.getId())).isEmpty();
        assertThat(financialAccountRepository.findByUser_IdOrderByNameAsc(user.getId())).isEmpty();
    }

    @Test
    void delete_shouldRejectWhenReferencedByTransaction() throws Exception {
        User user = createUser("account-transaction@test.com", "Transaction User");
        FinancialAccount account = new FinancialAccount();
        account.setUser(user);
        account.setName("Checking");
        account.setType(FinancialAccountType.BANK_ACCOUNT);
        account.setCurrency("USD");
        account = financialAccountRepository.save(account);

        Category category = new Category();
        category.setName("Bills");
        category.setType(com.api.enums.CategoryType.EXPENSE);
        category.setUserId(user.getId());
        category = categoryRepository.save(category);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAccount(account);
        transaction.setType(com.api.enums.TransactionType.EXPENSE);
        transaction.setAmount(BigDecimal.valueOf(25.5));
        transaction.setCurrency("USD");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setSource(com.api.enums.TransactionSource.MANUAL);
        transaction.setMerchantName("Merchant");
        transactionRepository.save(transaction);

        Cookie accessCookie = loginAndGetAccessCookie("account-transaction@test.com");

        mockMvc.perform(delete("/api/v1/accounts/{accountId}", account.getId())
                        .cookie(accessCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete financial account because it is referenced by one or more transactions"));
    }

    private Cookie loginAndGetAccessCookie(String email) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.api.dto.LoginRequest(email, "SecurePassword123!")
                        )))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();
        return accessCookie;
    }

    private User createUser(String email, String fullName) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("SecurePassword123!"));
        user.setFullName(fullName);
        user.setDefaultCurrency("USD");
        user.setTimezone("Europe/Paris");
        return userRepository.save(user);
    }
}
