package com.parking.repository;

import com.parking.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private final MongoOperations mongoOperations;

    @Override
    public BigDecimal sumRevenueByPeriod(LocalDateTime from, LocalDateTime to) {
        Query query = new Query(Criteria.where("paymentTime").gte(from).lte(to)
                .and("paymentStatus").is("Success"));
        List<Payment> payments = mongoOperations.find(query, Payment.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Payment p : payments) {
            if (p.getAmount() != null) {
                total = total.add(p.getAmount());
            }
        }
        return total;
    }
}
