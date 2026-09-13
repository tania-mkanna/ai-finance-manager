package com.api.controller;

import com.api.dto.CreateCategoryRequest;
import com.api.enums.CategoryType;
import com.api.model.Category;
import com.api.model.FinancialAccount;
import com.api.model.Transaction;
import com.api.model.User;
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
class CategoryControllerIntegrationTest extends BaseIntegrationTest {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FinancialAccountRepository financialAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Cookie loginAndGetAccessCookie(String email) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.api.dto.LoginRequest(
                                        email,
                                        "SecurePassword123!"
                                )
                        )))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();

        return accessCookie;
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        transactionRepository.deleteAll();
        financialAccountRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }
    @Test
    void list_shouldReturnActiveSystemAndCurrentUserCategories() throws Exception {
        User user = createUser("category-list@test.com", "List User");

        Category systemCategory = new Category();
        systemCategory.setName("Salary");
        systemCategory.setType(CategoryType.INCOME);
        systemCategory.setUserId(null);
        categoryRepository.save(systemCategory);

        Category customCategory = new Category();
        customCategory.setName("Groceries");
        customCategory.setType(CategoryType.EXPENSE);
        customCategory.setUserId(user.getId());
        customCategory.setIcon("shopping");
        customCategory.setColor("#ff0000");
        categoryRepository.save(customCategory);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.api.dto.LoginRequest("category-list@test.com", "SecurePassword123!"))))
                .andExpect(status().isOk())
                .andReturn();

        var accessCookie = loginResult.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();

        mockMvc.perform(get("/api/v1/categories")
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Salary"))
                .andExpect(jsonPath("$[1].name").value("Groceries"));
    }

    @Test
    void create_shouldPersistCustomCategoryAndIgnoreClientUserId() throws Exception {
        User user = createUser("category-create@test.com", "Create User");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.api.dto.LoginRequest("category-create@test.com", "SecurePassword123!"))))
                .andExpect(status().isOk())
                .andReturn();

        var accessCookie = loginResult.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();

        CreateCategoryRequest request = new CreateCategoryRequest(
                "Travel",
                CategoryType.EXPENSE,
                "plane",
                "#123456"
        );

        mockMvc.perform(post("/api/v1/categories")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Travel"))
                .andExpect(jsonPath("$.isSystem").value(false));

        Category saved = categoryRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
    }
    @Test
    void create_shouldRejectNameLongerThan255Characters() throws Exception {
        createUser("category-long-name@test.com", "Long Name User");

        Cookie accessCookie =
                loginAndGetAccessCookie("category-long-name@test.com");

        String longName = "a".repeat(256);

        CreateCategoryRequest request = new CreateCategoryRequest(
                longName,
                CategoryType.EXPENSE,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/categories")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name")
                        .value("Category name must be between 1 and 255 characters"));
    }
    @Test
    void delete_shouldRejectIfCategoryIsReferencedByTransaction() throws Exception {
        User user = createUser("category-delete@test.com", "Delete User");

        Category category = new Category();
        category.setName("Bills");
        category.setType(CategoryType.EXPENSE);
        category.setUserId(user.getId());
        categoryRepository.save(category);

        FinancialAccount account = new FinancialAccount();
        account.setUser(user);
        account.setName("Checking");
        account.setType(com.api.enums.FinancialAccountType.BANK_ACCOUNT);
        account.setCurrency("USD");
        account = financialAccountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAccount(account);
        transaction.setType(com.api.enums.TransactionType.EXPENSE);
        transaction.setAmount(BigDecimal.valueOf(25.50));
        transaction.setCurrency("USD");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setSource(com.api.enums.TransactionSource.MANUAL);
        transaction.setMerchantName("Test Merchant");
        transactionRepository.save(transaction);

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.api.dto.LoginRequest("category-delete@test.com", "SecurePassword123!"))))
                .andExpect(status().isOk())
                .andReturn();

        var accessCookie = loginResult.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", category.getId())
                        .cookie(accessCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete category because it is referenced by one or more transactions"));
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
