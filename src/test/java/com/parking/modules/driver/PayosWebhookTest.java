package com.parking.modules.driver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.common.exception.BusinessRuleException;
import com.parking.entity.Payment;
import com.parking.repository.PaymentRepository;
import com.parking.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PayosWebhookTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private PayosService payosService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(payosService, "checksumKey", "test-checksum-key");
    }

    @Test
    void testWebhookRejectsInvalidSignature() throws Exception {
        // Given
        String json = """
        {
          "signature": "invalid-sig",
          "data": {
            "orderCode": 12345,
            "amount": 2000,
            "description": "Thanh toan coc"
          }
        }
        """;
        JsonNode body = objectMapper.readTree(json);

        // When & Then
        assertThrows(BusinessRuleException.class, () -> payosService.handleWebhook(body), "Chu ky webhook khong hop le");
    }

    @Test
    void testWebhookIdempotency() throws Exception {
        // Given - generate a valid signature for the data
        // For testing, we can mock the internal hmac generation or just test the flow after signature validation.
        // Actually, since signature generation uses private method, let's create a real valid signature.
        String json = """
        {
          "data": {
            "amount": 2000,
            "description": "Thanh toan coc",
            "orderCode": 12345
          }
        }
        """;
        JsonNode data = objectMapper.readTree(json).path("data");
        String validSig = ReflectionTestUtils.invokeMethod(payosService, "signOfData", data);
        
        String fullJson = String.format("""
        {
          "signature": "%s",
          "data": {
            "amount": 2000,
            "description": "Thanh toan coc",
            "orderCode": 12345
          }
        }
        """, validSig);
        JsonNode body = objectMapper.readTree(fullJson);

        Payment payment = new Payment();
        payment.setPaymentStatus("Success"); // Already processed
        
        when(paymentRepository.findFirstByTransactionReference("12345")).thenReturn(Optional.of(payment));

        // When
        payosService.handleWebhook(body);

        // Then
        // Should return early and not save anything
        verify(paymentRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }
}
