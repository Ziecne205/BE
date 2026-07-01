package com.parking.modules.manager;

import java.util.List;

/**
 * Tinh trang cho trong toan bai theo loai xe (headroom real-time) - Phan he 1.
 * Khop shape LotAvailability ben FE (byVehicleType[]).
 */
public record AvailabilityResponse(List<VehicleTypeAvailability> byVehicleType) {

    public record VehicleTypeAvailability(
            String vehicleTypeName,
            long capacity,        // C - so o kha dung (tru Maintenance)
            long inside,          // dang trong bai (phien mo)
            long outstanding,     // booking da xac nhan chua vao
            long walkInHeadroom,  // C - inside - outstanding (co the am khi vuot suc chua)
            List<Zone> byZone) {
    }

    public record Zone(String zone, long available) {
    }
}
