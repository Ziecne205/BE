package com.parking.config.mongo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Cung cap MongoTransactionManager de @Transactional va TransactionTemplate hoat dong.
 * LUU Y: transaction da document chi chay tren replica set (MongoDB Atlas M0 la replica set,
 * nen OK). Neu chay Mongo standalone (khong replica), cac transaction se bao loi.
 */
@Configuration
@EnableTransactionManagement
public class MongoConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
