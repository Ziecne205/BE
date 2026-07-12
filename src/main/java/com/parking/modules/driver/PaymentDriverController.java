package com.parking.modules.driver;

import com.parking.common.ApiResponse;
import com.parking.entity.Payment;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/payments")
@RequiredArgsConstructor
@Tag(name = "Driver - Payments", description = "Thanh toan truc tuyen - phan he Khach hang")
// Khop voi ReservationController (hasAnyRole DRIVER/MANAGER/ADMIN): ai tao duoc booking thi
// phai tra duoc coc. Truoc day chi DRIVER -> Manager/Admin tao booking xong bi 403 o buoc
// thanh toan ("Ban khong co quyen thuc hien hanh dong nay"). Quyen so huu van duoc kiem tra
// trong PayosService/PaymentDriverService (chi tra duoc cho booking/phien cua chinh minh).
@PreAuthorize("hasAnyRole('DRIVER', 'MANAGER', 'ADMIN')")
public class PaymentDriverController {

    private final PaymentDriverService paymentDriverService;
    private final PayosService payosService;

    @PostMapping("/payos/create-link")
    public ApiResponse<PayosLinkResponse> createPayosLink(@RequestBody PayosLinkRequest request, Authentication auth) {
        // DEPOSIT: id = reservationId (tien coc dat cho). PARKING: id = sessionId (phi gui xe phien dang do).
        if ("DEPOSIT".equalsIgnoreCase(request.getType())) {
            return ApiResponse.ok("Tao link PayOS thanh cong",
                    payosService.createDepositLink(request.getId(), auth.getName()));
        }
        if ("PARKING".equalsIgnoreCase(request.getType())) {
            return ApiResponse.ok("Tao link PayOS thanh cong",
                    paymentDriverService.createParkingLink(request.getId(), auth.getName()));
        }
        return ApiResponse.fail("Loai thanh toan khong hop le (chi ho tro DEPOSIT hoac PARKING)");
    }

    @PostMapping("/checkout")
    public ApiResponse<String> checkout(@RequestBody PaymentRequest request, Authentication auth) {
        return ApiResponse.ok("URL thanh toan duoc tao thanh cong", paymentDriverService.createMockPaymentUrl(request, auth.getName()));
    }

    @PostMapping("/mock-callback")
    public ApiResponse<Payment> mockCallback(@RequestParam String txnRef, @RequestParam Long sessionId, @RequestParam String status, Authentication auth) {
        return ApiResponse.ok("Cap nhat thanh toan thanh cong", paymentDriverService.processMockCallback(txnRef, sessionId, status, auth.getName()));
    }
}
