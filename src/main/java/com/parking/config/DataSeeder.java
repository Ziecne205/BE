package com.parking.config;

import com.parking.entity.*;
import com.parking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nap du lieu nen khi database trong (Mongo Atlas moi tao khong co san du lieu nhu ParkingDB cu).
 * Chi chay khi CHUA co Role nao -> an toan, khong ghi de du lieu that. Sau lan seed dau tien,
 * moi lan khoi dong sau se bo qua.
 *
 * Tat ca tai khoan seed dung chung mat khau "123456" (bcrypt hash ben duoi da co san tu du an cu).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    /** bcrypt hash cua mat khau "123456". */
    private static final String DEMO_HASH = "$2a$10$NjorPjRHjb0/OrP.FHlE3udueGRFgrNm4boM4iSoZeFhisL64RcOG";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final FloorRepository floorRepository;
    private final GateRepository gateRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            seedCore();
        }
        if (permissionRepository.count() == 0) {
            seedPermissions();
        }
    }

    private void seedCore() {
        log.info("Database trong -> bat dau seed du lieu nen...");

        // 1) Roles
        Role admin = roleRepository.save(role("Admin", "Quan tri he thong"));
        Role manager = roleRepository.save(role("Manager", "Quan ly bai xe"));
        Role staff = roleRepository.save(role("Staff", "Nhan vien tai cong"));
        Role driver = roleRepository.save(role("Driver", "Tai xe"));

        // 2) Tai khoan demo (khop voi du an cu: *@parking.vn, mat khau 123456)
        userRepository.save(user("admin@parking.vn", "Quan Tri Vien", "0900000001", "khoicongviec@gmail.com", admin));
        userRepository.save(user("manager@parking.vn", "Quan Ly", "0900000002", "khoikiet130@gmail.com", manager));
        userRepository.save(user("staff@parking.vn", "Nhan Vien", "0900000003", "khoiislearning@gmail.com", staff));
        userRepository.save(user("driver@parking.vn", "Tai Xe Demo", "0900000004", "nguyenkhoi2004vt@gmail.com", driver));

        // 3) Loai xe (FE loc theo ten chua "o to")
        VehicleType car = vehicleTypeRepository.save(VehicleType.builder()
                .typeName("Ô tô").dimensions("4.5m x 1.8m").build());

        // 4) Bang gia (gia phang 10.000 VND/gio)
        pricingPolicyRepository.save(PricingPolicy.builder()
                .vehicleType(car)
                .basePrice(new BigDecimal("10000"))
                .baseHours(1)
                .extraHourPrice(new BigDecimal("10000"))
                .nightSurcharge(BigDecimal.ZERO)
                .lostTicketFee(new BigDecimal("50000"))
                .effectiveDate(LocalDateTime.now())
                .status("Active")
                .build());

        // 5) Tang + cong + o do
        Floor floorG = floorRepository.save(floor("Tầng G", null, 6));
        Floor floorB1 = floorRepository.save(floor("Tầng B1", car, 10));
        Floor floorB2 = floorRepository.save(floor("Tầng B2", car, 8));

        gateRepository.save(gate("Cổng Vào Chính", "Entry", floorG));
        gateRepository.save(gate("Cổng Ra Chính", "Exit", floorG));
        gateRepository.save(gate("Cổng Vào Hầm B1", "Entry", floorB1));
        gateRepository.save(gate("Cổng Ra Hầm B1", "Exit", floorB1));
        gateRepository.save(gate("Cổng Vào Hầm B2", "Entry", floorB2));
        gateRepository.save(gate("Cổng Ra Hầm B2", "Exit", floorB2));

        List<ParkingSlot> slots = new ArrayList<>();
        slots.addAll(slotsForFloor(floorG, car, "G-A", 6));
        slots.addAll(slotsForFloor(floorB1, car, "B1-A", 10));
        slots.addAll(slotsForFloor(floorB2, car, "B2-A", 8));
        parkingSlotRepository.saveAll(slots);

        log.info("Seed xong: {} role, {} user, 1 loai xe, 3 tang, 6 cong, {} o do.",
                roleRepository.count(), userRepository.count(), slots.size());
        log.info("Tai khoan demo (mat khau 123456): admin@parking.vn / manager@parking.vn / staff@parking.vn / driver@parking.vn");
    }

    /**
     * Nap catalog quyen han (Permissions) va gan mac dinh cho tung role.
     * Tach rieng khoi seedCore() vi du lieu Role/User co the da ton tai tu truoc
     * (vd Mongo Atlas cu chua co Permissions) -> van phai chay de bo sung du lieu con thieu.
     */
    private void seedPermissions() {
        log.info("Permissions collection trong -> bat dau seed catalog quyen han...");

        Permission dashboardView = savePermission("dashboard.view", "Xem trung tam giam sat");
        Permission usersManage = savePermission("users.manage", "Quan ly tai khoan nguoi dung");
        Permission rbacManage = savePermission("rbac.manage", "Quan ly phan quyen vai tro");
        Permission systemConfigManage = savePermission("system_config.manage", "Quan ly cau hinh he thong");
        Permission auditLogView = savePermission("audit_log.view", "Xem nhat ky he thong");

        Permission floorsManage = savePermission("floors.manage", "Quan ly tang va o do");
        Permission gatesManage = savePermission("gates.manage", "Quan ly cong ra vao");
        Permission pricingManage = savePermission("pricing.manage", "Quan ly bang gia");
        Permission vehicleTypesManage = savePermission("vehicle_types.manage", "Quan ly loai xe");
        Permission quotasManage = savePermission("quotas.manage", "Quan ly han muc dat cho");
        Permission feeConfigManage = savePermission("fee_config.manage", "Quan ly cau hinh phi");
        Permission reservationsManage = savePermission("reservations.manage", "Quan ly dat cho");
        Permission incidentsManage = savePermission("incidents.manage", "Quan ly su co");
        Permission paymentsManage = savePermission("payments.manage", "Quan ly thanh toan");
        Permission reportsView = savePermission("reports.view", "Xem bao cao");

        Permission checkinManage = savePermission("checkin.manage", "Thuc hien check-in");
        Permission checkoutManage = savePermission("checkout.manage", "Thuc hien check-out");
        Permission sessionsView = savePermission("sessions.view", "Xem phien do xe dang hoat dong");

        Set<Permission> adminPermissions = new HashSet<>(List.of(
                dashboardView, usersManage, rbacManage, systemConfigManage, auditLogView,
                floorsManage, gatesManage, pricingManage, vehicleTypesManage, quotasManage,
                feeConfigManage, reservationsManage, incidentsManage, paymentsManage, reportsView,
                checkinManage, checkoutManage, sessionsView));

        Set<Permission> managerPermissions = new HashSet<>(List.of(
                dashboardView, floorsManage, gatesManage, pricingManage, vehicleTypesManage,
                quotasManage, feeConfigManage, reservationsManage, incidentsManage, paymentsManage,
                reportsView));

        Set<Permission> staffPermissions = new HashSet<>(List.of(
                checkinManage, checkoutManage, sessionsView, incidentsManage));

        assignPermissionsToRole("Admin", adminPermissions);
        assignPermissionsToRole("Manager", managerPermissions);
        assignPermissionsToRole("Staff", staffPermissions);
        // Driver: khong co quyen quan tri nao -> khong gan gi ca.

        log.info("Seed xong: {} permission.", permissionRepository.count());
    }

    private Permission savePermission(String code, String description) {
        return permissionRepository.save(Permission.builder()
                .permissionCode(code)
                .description(description)
                .build());
    }

    private void assignPermissionsToRole(String roleName, Set<Permission> permissions) {
        roleRepository.findByRoleName(roleName).ifPresent(role -> {
            role.setPermissions(permissions);
            roleRepository.save(role);
        });
    }

    private Role role(String name, String desc) {
        return Role.builder().roleName(name).description(desc).build();
    }

    private User user(String username, String fullName, String phone, String email, Role role) {
        return User.builder()
                .username(username)
                .passwordHash(DEMO_HASH)
                .fullName(fullName)
                .phoneNumber(phone)
                .email(email)
                .role(role)
                .status("Active")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .consecutiveNoShows(0)
                .blacklisted(false)
                .build();
    }

    private Floor floor(String name, VehicleType dedicated, int capacity) {
        return Floor.builder().floorName(name).dedicatedVehicleType(dedicated).totalCapacity(capacity).build();
    }

    private Gate gate(String name, String type, Floor floor) {
        return Gate.builder().gateName(name).gateType(type).floor(floor).build();
    }

    private List<ParkingSlot> slotsForFloor(Floor floor, VehicleType type, String prefix, int count) {
        List<ParkingSlot> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(ParkingSlot.builder()
                    .floor(floor)
                    .zone("A")
                    .slotCode(String.format("%s%02d", prefix, i))
                    .vehicleType(type)
                    .status("Available")
                    .build());
        }
        return list;
    }
}
