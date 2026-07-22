package com.parking.config.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Gan ID truoc khi ghi cho cac entity chua co ID (mo phong @GeneratedValue cua JPA):
 *  - ID kieu Long/Integer -> lay tu SequenceGeneratorService (tu tang theo collection)
 *  - ID kieu UUID          -> sinh ngau nhien
 *  - ID kieu String        -> bo qua (khoa nghiep vu, do code tu gan, vd SystemConfig)
 */
@Component
@RequiredArgsConstructor
public class IdGeneratingListener extends AbstractMongoEventListener<Object> {

    private final SequenceGeneratorService sequenceGenerator;
    private final MongoMappingContext mappingContext;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<Object> event) {
        Object source = event.getSource();
        if (source == null) {
            return;
        }

        MongoPersistentEntity<?> entity = mappingContext.getPersistentEntity(source.getClass());
        if (entity == null) {
            return;
        }
        MongoPersistentProperty idProperty = entity.getIdProperty();
        if (idProperty == null) {
            return;
        }

        PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(source);
        Object currentId = accessor.getProperty(idProperty);
        if (currentId != null) {
            return; // da co ID -> khong dong vao (update)
        }

        Class<?> type = idProperty.getType();
        if (type == UUID.class) {
            accessor.setProperty(idProperty, UUID.randomUUID());
        } else if (type == Long.class || type == long.class) {
            accessor.setProperty(idProperty, sequenceGenerator.generateSequence(entity.getCollection()));
        } else if (type == Integer.class || type == int.class) {
            accessor.setProperty(idProperty, (int) sequenceGenerator.generateSequence(entity.getCollection()));
        }
        // String id (SystemConfig...) -> khong tu sinh
    }
}
