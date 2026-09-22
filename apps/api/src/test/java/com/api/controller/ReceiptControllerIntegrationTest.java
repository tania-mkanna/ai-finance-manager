package com.api.controller;

import com.api.model.Category;
import com.api.model.Receipt;
import com.api.model.ReceiptItem;
import com.api.model.User;
import com.api.repository.CategoryRepository;
import com.api.repository.ReceiptItemRepository;
import com.api.repository.ReceiptRepository;
import com.api.repository.UserRepository;
import com.api.enums.CategoryType;
import com.api.enums.ReceiptStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReceiptControllerIntegrationTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private ReceiptItemRepository receiptItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        receiptItemRepository.deleteAll();
        receiptRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void upload_shouldPersistReceiptWithUploadedStatusAndAutoAssignUser() throws Exception {
        User user = createUser("receipt-upload@test.com", "Upload User");
        Cookie accessCookie = loginAndGetAccessCookie("receipt-upload@test.com");

        byte[] jpegBytes = new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                (byte) 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00,
                0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00,
                0x00, (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00,
                (byte) 0x08, 0x06, 0x06, 0x07, 0x06, 0x05, 0x08,
                0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C,
                0x14, 0x0D, 0x0C, 0x0B, 0x0B, 0x0D, 0x1F, 0x1F,
                0x1F, 0x13, 0x17, 0x18, 0x17, 0x10, 0x16, 0x12,
                0x13, 0x1D, 0x1D, 0x1D, 0x1A, 0x1B, 0x1C, 0x1C,
                0x1C, 0x1C, 0x1C, 0x1D, 0x1C, 0x1D, 0x1D, 0x1D,
                (byte) 0xFF, (byte) 0xC0, 0x00, 0x11, 0x08, 0x00,
                0x01, 0x00, 0x01, 0x00, 0x03, 0x01, 0x22, 0x00,
                0x02, 0x11, 0x01, 0x03, 0x11, 0x01, (byte) 0xFF,
                (byte) 0xDA, 0x00, 0x0C, 0x03, 0x01, 0x00, 0x02,
                0x11, 0x03, 0x11, 0x00, 0x3F, 0x00, (byte) 0xFC,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                (byte) 0xFF, (byte) 0xD9
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                jpegBytes
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .param("currency", "usd")
                        .cookie(accessCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("receipt.jpg"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.id").exists());

        Receipt saved = receiptRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getStatus()).isEqualTo(ReceiptStatus.UPLOADED);
        assertThat(saved.getProcessedAt()).isNull();
        assertThat(saved.getErrorMessage()).isNull();
    }
    @Test
    void upload_shouldRejectUnsupportedMediaType() throws Exception {
        createUser("receipt-media@test.com", "Media User");
        Cookie accessCookie = loginAndGetAccessCookie("receipt-media@test.com");

        mockMvc.perform(post("/api/v1/receipts")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message")
                        .value("Unsupported media type"));
    }

    @Test
    void upload_shouldRejectMissingEmptyAndInvalidFiles() throws Exception {
        createUser("receipt-invalid@test.com", "Invalid User");
        Cookie accessCookie = loginAndGetAccessCookie("receipt-invalid@test.com");

        mockMvc.perform(multipart("/api/v1/receipts")
                        .cookie(accessCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Receipt file is required"));

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]))
                        .cookie(accessCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Receipt file cannot be empty"));

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes()))
                        .cookie(accessCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported receipt file type"));
    }

    @Test
    void getOne_shouldReturnReceiptAndItemsOnlyForOwner() throws Exception {
        User user = createUser("receipt-get@test.com", "Receipt Getter");
        User otherUser = createUser("receipt-other@test.com", "Other User");
        Cookie accessCookie = loginAndGetAccessCookie("receipt-get@test.com");

        Category category = new Category();
        category.setUserId(user.getId());
        category.setName("Groceries");
        category.setType(CategoryType.EXPENSE);
        category = categoryRepository.save(category);

        Receipt receipt = new Receipt();
        receipt.setUser(user);
        receipt.setFileUrl("receipts/test.jpg");
        receipt.setFileName("test.jpg");
        receipt.setMimeType("image/jpeg");
        receipt.setStatus(ReceiptStatus.UPLOADED);
        receipt.setReceiptDate(LocalDate.of(2026, 8, 26));
        receipt.setMerchantName("Carrefour");
        receipt.setTotalAmount(new BigDecimal("52.75"));
        receipt.setCurrency("USD");
        receipt = receiptRepository.save(receipt);

        ReceiptItem item = new ReceiptItem();
        item.setReceipt(receipt);
        item.setName("Milk");
        item.setQuantity(new BigDecimal("1"));
        item.setUnitPrice(new BigDecimal("5.00"));
        item.setTotalPrice(new BigDecimal("5.00"));
        item.setCategory(category);
        item.setConfidenceScore(new BigDecimal("0.98"));
        receiptItemRepository.save(item);

        Receipt otherReceipt = new Receipt();
        otherReceipt.setUser(otherUser);
        otherReceipt.setFileUrl("receipts/other.jpg");
        otherReceipt.setFileName("other.jpg");
        otherReceipt.setMimeType("image/jpeg");
        otherReceipt.setStatus(ReceiptStatus.UPLOADED);
        otherReceipt.setCurrency("USD");
        otherReceipt = receiptRepository.save(otherReceipt);

        mockMvc.perform(get("/api/v1/receipts/{receiptId}", receipt.getId())
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(receipt.getId().toString()))
                .andExpect(jsonPath("$.merchantName").value("Carrefour"))
                .andExpect(jsonPath("$.items[0].name").value("Milk"))
                .andExpect(jsonPath("$.items[0].category.id").value(category.getId().toString()))
                .andExpect(jsonPath("$.fileUrl").doesNotExist());

        mockMvc.perform(get("/api/v1/receipts/{receiptId}", otherReceipt.getId())
                        .cookie(accessCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_shouldFilterCurrentUserReceiptsAndPageParameters() throws Exception {
        User user = createUser("receipt-list@test.com", "List User");
        User otherUser = createUser("receipt-list-other@test.com", "Other User");
        Cookie accessCookie = loginAndGetAccessCookie("receipt-list@test.com");

        Receipt ownOld = new Receipt();
        ownOld.setUser(user);
        ownOld.setFileUrl("receipts/a.jpg");
        ownOld.setFileName("a.jpg");
        ownOld.setMimeType("image/jpeg");
        ownOld.setStatus(ReceiptStatus.UPLOADED);
        ownOld.setReceiptDate(LocalDate.of(2026, 1, 10));
        ownOld.setCurrency("USD");
        receiptRepository.save(ownOld);

        Receipt ownNew = new Receipt();
        ownNew.setUser(user);
        ownNew.setFileUrl("receipts/b.jpg");
        ownNew.setFileName("b.jpg");
        ownNew.setMimeType("image/jpeg");
        ownNew.setStatus(ReceiptStatus.REVIEW_REQUIRED);
        ownNew.setReceiptDate(LocalDate.of(2026, 2, 10));
        ownNew.setCurrency("USD");
        receiptRepository.save(ownNew);

        Receipt foreign = new Receipt();
        foreign.setUser(otherUser);
        foreign.setFileUrl("receipts/c.jpg");
        foreign.setFileName("c.jpg");
        foreign.setMimeType("image/jpeg");
        foreign.setStatus(ReceiptStatus.UPLOADED);
        foreign.setCurrency("EUR");
        receiptRepository.save(foreign);

        mockMvc.perform(get("/api/v1/receipts")
                        .cookie(accessCookie)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "UPLOADED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("UPLOADED"));

        mockMvc.perform(get("/api/v1/receipts")
                        .cookie(accessCookie)
                        .param("page", "0")
                        .param("size", "1")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .param("sort", "receiptDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].receiptDate").value("2026-01-10"));

        mockMvc.perform(get("/api/v1/receipts")
                        .cookie(accessCookie)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_shouldUpdateReceiptAndItemsAndRejectForeignOwnership() throws Exception {
        User user = createUser("receipt-patch@test.com", "Patch User");
        Category category = new Category();
        category.setUserId(user.getId());
        category.setName("Food");
        category.setType(CategoryType.EXPENSE);
        category = categoryRepository.save(category);

        Receipt receipt = new Receipt();
        receipt.setUser(user);
        receipt.setFileUrl("receipts/patch.jpg");
        receipt.setFileName("patch.jpg");
        receipt.setMimeType("image/jpeg");
        receipt.setStatus(ReceiptStatus.REVIEW_REQUIRED);
        receipt.setCurrency("USD");
        receipt = receiptRepository.save(receipt);

        ReceiptItem existingItem = new ReceiptItem();
        existingItem.setReceipt(receipt);
        existingItem.setName("Old item");
        existingItem.setQuantity(new BigDecimal("1"));
        existingItem.setUnitPrice(new BigDecimal("2.00"));
        existingItem.setTotalPrice(new BigDecimal("2.00"));
        existingItem = receiptItemRepository.save(existingItem);

        String requestBody = "{\"receiptDate\":\"2026-08-26\",\"merchantName\":\"Carrefour\",\"totalAmount\":52.75,\"currency\":\"usd\",\"items\":[{\"id\":\"" + existingItem.getId() + "\",\"name\":\"Milk\",\"quantity\":1,\"unitPrice\":5.00,\"totalPrice\":5.00,\"categoryId\":\"" + category.getId() + "\"},{\"name\":\"Bread\",\"quantity\":2,\"unitPrice\":3.00,\"totalPrice\":6.00,\"categoryId\":\"" + category.getId() + "\"}]}";

        Cookie accessCookie = loginAndGetAccessCookie("receipt-patch@test.com");

        mockMvc.perform(patch("/api/v1/receipts/{receiptId}", receipt.getId())
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantName").value("Carrefour"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(patch("/api/v1/receipts/{receiptId}", receipt.getId())
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void delete_shouldRemoveReceiptAndFileOnlyWhenAllowed() throws Exception {
        User user = createUser("receipt-delete@test.com", "Delete User");
        Cookie accessCookie = loginAndGetAccessCookie("receipt-delete@test.com");

        Receipt receipt = new Receipt();
        receipt.setUser(user);
        receipt.setFileUrl("receipts/delete-delete.jpg");
        receipt.setFileName("delete.jpg");
        receipt.setMimeType("image/jpeg");
        receipt.setStatus(ReceiptStatus.UPLOADED);
        receipt.setCurrency("USD");
        receipt = receiptRepository.save(receipt);

        mockMvc.perform(delete("/api/v1/receipts/{receiptId}", receipt.getId())
                        .cookie(accessCookie))
                .andExpect(status().isNoContent());

        assertThat(receiptRepository.findById(receipt.getId())).isEmpty();
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

    private Cookie loginAndGetAccessCookie(String email) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.api.dto.LoginRequest(email, "SecurePassword123!"))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();
        return accessCookie;
    }
}
