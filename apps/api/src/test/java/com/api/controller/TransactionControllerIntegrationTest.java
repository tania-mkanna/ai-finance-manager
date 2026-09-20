package com.api.controller;

import com.api.dto.CreateTransactionRequest;
import com.api.dto.UpdateTransactionRequest;
import com.api.enums.CategoryType;
import com.api.enums.TransactionSource;
import com.api.enums.TransactionType;
import com.api.model.Category;
import com.api.model.FinancialAccount;
import com.api.model.Transaction;
import com.api.model.User;
import com.api.repository.CategoryRepository;
import com.api.repository.FinancialAccountRepository;
import com.api.repository.TransactionRepository;
import com.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionControllerIntegrationTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FinancialAccountRepository financialAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

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
    void create_shouldPersistTransactionWithManualDefaults() throws Exception {
        User user = createUser("transactions-create@test.com", "Create User");
        Category category = createCategory(user.getId(), "Groceries", CategoryType.EXPENSE);
        FinancialAccount account = createAccount(user, "Checking", "USD");
        Cookie accessCookie = loginAndGetAccessCookie("transactions-create@test.com");

        CreateTransactionRequest request = new CreateTransactionRequest(
                category.getId(),
                account.getId(),
                TransactionType.EXPENSE,
                new BigDecimal("75.50"),
                "usd",
                LocalDateTime.of(2025, 1, 15, 12, 30),
                "Fresh Market",
                "Weekly groceries"
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.receiptId").doesNotExist())
                .andExpect(jsonPath("$.category.id").value(category.getId().toString()))
                .andExpect(jsonPath("$.account.id").value(account.getId().toString()))
                .andExpect(jsonPath("$.amount").value(75.5))
                .andExpect(jsonPath("$.currency").value("USD"));

        assertThat(transactionRepository.findAll()).hasSize(1);
        Transaction saved = transactionRepository.findAll().getFirst();
        assertThat(saved.getSource()).isEqualTo(TransactionSource.MANUAL);
        assertThat(saved.getReceipt()).isNull();
    }

    @Test
    void create_shouldAllowSystemCategoryAndRejectCategoryTypeMismatch() throws Exception {
        User user = createUser("transactions-category@test.com", "Category User");
        Category systemCategory = new Category();
        systemCategory.setName("Salary");
        systemCategory.setType(CategoryType.INCOME);
        systemCategory.setUserId(null);
        systemCategory = categoryRepository.save(systemCategory);

        FinancialAccount account = createAccount(user, "Wallet", "USD");
        Cookie accessCookie = loginAndGetAccessCookie("transactions-category@test.com");

        CreateTransactionRequest incomeRequest = new CreateTransactionRequest(
                systemCategory.getId(),
                account.getId(),
                TransactionType.INCOME,
                new BigDecimal("1250.00"),
                "USD",
                LocalDateTime.now(),
                null,
                null
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isCreated());

        Category mismatchCategory = createCategory(user.getId(), "Rent", CategoryType.INCOME);
        CreateTransactionRequest mismatchRequest = new CreateTransactionRequest(
                mismatchCategory.getId(),
                account.getId(),
                TransactionType.EXPENSE,
                new BigDecimal("200"),
                "USD",
                LocalDateTime.now(),
                "Landlord",
                null
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category type is not compatible with transaction type"));
    }

    @Test
    void create_shouldRejectAnotherUsersCategoryOrAccount() throws Exception {
        User owner = createUser("transactions-owner@test.com", "Owner");
        User other = createUser("transactions-other@test.com", "Other");
        Category ownerCategory = createCategory(owner.getId(), "Dining", CategoryType.EXPENSE);
        Category otherCategory = createCategory(other.getId(), "Other Dining", CategoryType.EXPENSE);
        FinancialAccount ownerAccount = createAccount(owner, "Savings", "USD");
        FinancialAccount otherAccount = createAccount(other, "Wallet", "USD");

        Cookie accessCookie = loginAndGetAccessCookie("transactions-other@test.com");

        CreateTransactionRequest categoryRequest = new CreateTransactionRequest(
                ownerCategory.getId(),
                otherAccount.getId(),
                TransactionType.EXPENSE,
                new BigDecimal("10"),
                "USD",
                LocalDateTime.now(),
                "Lunch",
                null
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this category"));

        CreateTransactionRequest accountRequest = new CreateTransactionRequest(
                otherCategory.getId(),
                ownerAccount.getId(),
                TransactionType.EXPENSE,
                new BigDecimal("10"),
                "USD",
                LocalDateTime.now(),
                "Lunch",
                null
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this financial account"));
    }

    @Test
    void create_shouldRejectInvalidAmountCurrencyAndEnum() throws Exception {
        User user = createUser("transactions-invalid@test.com", "Invalid User");
        Category category = createCategory(user.getId(), "Food", CategoryType.EXPENSE);
        FinancialAccount account = createAccount(user, "Cash", "USD");
        Cookie accessCookie = loginAndGetAccessCookie("transactions-invalid@test.com");

        mockMvc.perform(post("/api/v1/transactions")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + category.getId() + "\",\"accountId\":\"" + account.getId() + "\",\"type\":\"EXPENSE\",\"amount\":0,\"currency\":\"USD\",\"transactionDate\":\"2025-01-15T12:30:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").value("Amount must be greater than 0"));

        mockMvc.perform(post("/api/v1/transactions")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + category.getId() + "\",\"accountId\":\"" + account.getId() + "\",\"type\":\"EXPENSE\",\"amount\":25.5,\"currency\":\"US\",\"transactionDate\":\"2025-01-15T12:30:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.currency").value("Currency must be a valid 3-character ISO code"));

        mockMvc.perform(post("/api/v1/transactions")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"" + category.getId() + "\",\"accountId\":\"" + account.getId() + "\",\"type\":\"NOPE\",\"amount\":25.5,\"currency\":\"USD\",\"transactionDate\":\"2025-01-15T12:30:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid transaction type. Allowed values: INCOME, EXPENSE"));
    }

    @Test
    void list_shouldScopeToOwnerAndSupportFiltersPaginationAndSorting() throws Exception {
        User owner = createUser("transactions-list-owner@test.com", "Owner");
        User other = createUser("transactions-list-other@test.com", "Other");
        Category food = createCategory(owner.getId(), "Food", CategoryType.EXPENSE);
        Category salary = createCategory(owner.getId(), "Salary", CategoryType.INCOME);
        Category otherFood = createCategory(other.getId(), "Other Food", CategoryType.EXPENSE);
        FinancialAccount ownerAccount = createAccount(owner, "Checking", "USD");
        FinancialAccount ownerAccount2 = createAccount(owner, "Savings", "EUR");
        FinancialAccount otherAccount = createAccount(other, "Other Check", "USD");

        Transaction tx1 = createTransaction(owner, food, ownerAccount, TransactionType.EXPENSE, new BigDecimal("80.50"), "USD", LocalDateTime.of(2025, 1, 10, 9, 0), "Market", "Groceries");
        Transaction tx2 = createTransaction(owner, salary, ownerAccount2, TransactionType.INCOME, new BigDecimal("2500.00"), "EUR", LocalDateTime.of(2025, 2, 5, 8, 30), "Employer", "Paycheck");
        createTransaction(other, otherFood, otherAccount, TransactionType.EXPENSE, new BigDecimal("20.00"), "USD", LocalDateTime.now(), "Other Shop", "Hidden");

        Cookie accessCookie = loginAndGetAccessCookie("transactions-list-owner@test.com");

        mockMvc.perform(get("/api/v1/transactions")
                        .cookie(accessCookie)
                        .param("type", "EXPENSE")
                        .param("categoryId", food.getId().toString())
                        .param("accountId", ownerAccount.getId().toString())
                        .param("merchant", "market")
                        .param("minAmount", "50")
                        .param("maxAmount", "100")
                        .param("from", "2025-01-01T00:00:00")
                        .param("to", "2025-01-31T23:59:59")
                        .param("source", "MANUAL")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "transactionDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].merchantName").value("Market"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content[0].category.id").value(food.getId().toString()));

        mockMvc.perform(get("/api/v1/transactions")
                        .cookie(accessCookie)
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "amount,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));

        assertThat(transactionRepository.findAll()).hasSize(3);
    }

    @Test
    void list_shouldRejectInvalidRanges() throws Exception {
        User user = createUser("transactions-range@test.com", "Range User");
        createCategory(user.getId(), "Food", CategoryType.EXPENSE);
        FinancialAccount account = createAccount(user, "Meal Card", "USD");
        Cookie accessCookie = loginAndGetAccessCookie("transactions-range@test.com");

        mockMvc.perform(get("/api/v1/transactions")
                        .cookie(accessCookie)
                        .param("minAmount", "100")
                        .param("maxAmount", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("minAmount cannot be greater than maxAmount"));

        mockMvc.perform(get("/api/v1/transactions")
                        .cookie(accessCookie)
                        .param("from", "2025-02-10T00:00:00")
                        .param("to", "2025-02-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from date cannot be after to date"));
    }

    @Test
    void get_shouldReturnOwnForeignAndMissingTransactions() throws Exception {
        User owner = createUser("transactions-get-owner@test.com", "Owner");
        User other = createUser("transactions-get-other@test.com", "Other");
        Category category = createCategory(owner.getId(), "Travel", CategoryType.EXPENSE);
        FinancialAccount account = createAccount(owner, "Main Card", "USD");
        Transaction own = createTransaction(owner, category, account, TransactionType.EXPENSE, new BigDecimal("45"), "USD", LocalDateTime.now(), "Cab", "Ride");
        Transaction otherTx = createTransaction(other, createCategory(other.getId(), "Other Travel", CategoryType.EXPENSE), createAccount(other, "Other Card", "USD"), TransactionType.EXPENSE, new BigDecimal("80"), "USD", LocalDateTime.now(), "Taxi", "Foreign");

        Cookie accessCookie = loginAndGetAccessCookie("transactions-get-owner@test.com");

        mockMvc.perform(get("/api/v1/transactions/{transactionId}", own.getId())
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(own.getId().toString()));

        mockMvc.perform(get("/api/v1/transactions/{transactionId}", otherTx.getId())
                        .cookie(accessCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this transaction"));

        mockMvc.perform(get("/api/v1/transactions/{transactionId}", UUID.randomUUID())
                        .cookie(accessCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    @Test
    void patch_shouldUpdateProvidedFieldsAndRevalidateOwnershipAndCompatibility() throws Exception {
        User user = createUser("transactions-patch@test.com", "Patch User");
        Category category = createCategory(user.getId(), "Meals", CategoryType.EXPENSE);
        Category newCategory = createCategory(user.getId(), "Bonus", CategoryType.INCOME);
        FinancialAccount account = createAccount(user, "Cash", "USD");
        FinancialAccount newAccount = createAccount(user, "Card", "USD");
        Transaction transaction = createTransaction(user, category, account, TransactionType.EXPENSE, new BigDecimal("42.00"), "USD", LocalDateTime.of(2025, 4, 10, 12, 0), "Cafe", "Lunch");

        Cookie accessCookie = loginAndGetAccessCookie("transactions-patch@test.com");

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                newCategory.getId(),
                newAccount.getId(),
                TransactionType.EXPENSE,
                new BigDecimal("120.00"),
                "EUR",
                LocalDateTime.of(2025, 5, 1, 14, 0),
                "Paycheck",
                "Updated"
        );

        mockMvc.perform(patch("/api/v1/transactions/{transactionId}", transaction.getId())
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category type is not compatible with transaction type"));

        UpdateTransactionRequest partialRequest = new UpdateTransactionRequest(
                null,
                null,
                null,
                new BigDecimal("65.25"),
                "usd",
                null,
                "Corner Store",
                "Groceries"
        );

        mockMvc.perform(patch("/api/v1/transactions/{transactionId}", transaction.getId())
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(65.25))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.merchantName").value("Corner Store"));
    }

    @Test
    void delete_shouldSoftDeleteTransaction() throws Exception {
        User user = createUser("transactions-delete@test.com", "Delete User");
        Category category = createCategory(user.getId(), "Bills", CategoryType.EXPENSE);
        FinancialAccount account = createAccount(user, "Card", "USD");
        Transaction transaction = createTransaction(user, category, account, TransactionType.EXPENSE, new BigDecimal("19.99"), "USD", LocalDateTime.now(), "Utility", "Electricity");

        Cookie accessCookie = loginAndGetAccessCookie("transactions-delete@test.com");

        mockMvc.perform(delete("/api/v1/transactions/{transactionId}", transaction.getId())
                        .cookie(accessCookie))
                .andExpect(status().isNoContent());

        assertThat(transactionRepository.findById(transaction.getId())).isEmpty();
    }

    @Test
    void unauthenticatedRequests_shouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
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

    private Category createCategory(UUID userId, String name, CategoryType type) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setType(type);
        return categoryRepository.save(category);
    }

    private FinancialAccount createAccount(User user, String name, String currency) {
        FinancialAccount account = new FinancialAccount();
        account.setUser(user);
        account.setName(name);
        account.setType(com.api.enums.FinancialAccountType.CASH);
        account.setCurrency(currency);
        return financialAccountRepository.save(account);
    }

    private Transaction createTransaction(User user, Category category, FinancialAccount account, TransactionType type, BigDecimal amount, String currency, LocalDateTime date, String merchantName, String description) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAccount(account);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setTransactionDate(date);
        transaction.setMerchantName(merchantName);
        transaction.setDescription(description);
        transaction.setSource(TransactionSource.MANUAL);
        return transactionRepository.save(transaction);
    }

    private Cookie loginAndGetAccessCookie(String email) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.api.dto.LoginRequest(email, "SecurePassword123!"))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();
        return accessCookie;
    }
}
