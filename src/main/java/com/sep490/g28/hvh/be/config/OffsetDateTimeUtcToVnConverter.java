package com.sep490.g28.hvh.be.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.*;

/**
 * JPA AttributeConverter for handling {@link OffsetDateTime} timezone normalization.
 *
 * <p>Purpose:
 * - Ensure all datetime values are stored in the database in UTC.
 * - Automatically convert UTC values from the database to Vietnam timezone (Asia/Ho_Chi_Minh)
 *   when mapping to entity attributes.</p>
 *
 **/
@Converter(autoApply = true)
public class OffsetDateTimeUtcToVnConverter implements AttributeConverter<OffsetDateTime, OffsetDateTime> {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    public OffsetDateTime convertToDatabaseColumn(OffsetDateTime attribute) {
        if (attribute == null) return null;
        // convert VN -> UTC trước khi lưu DB
        return attribute.withOffsetSameInstant(ZoneOffset.UTC);
    }

    @Override
    public OffsetDateTime convertToEntityAttribute(OffsetDateTime dbData) {
        if (dbData == null) return null;
        // convert UTC -> VN khi đọc từ DB
        return dbData.withOffsetSameInstant(VN.getRules().getOffset(dbData.toInstant()));
    }
}
