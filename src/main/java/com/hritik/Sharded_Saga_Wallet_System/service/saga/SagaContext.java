// src/main/java/com/hritik/Sharded_Saga_Wallet_System/service/saga/SagaContext.java
package com.hritik.Sharded_Saga_Wallet_System.service.saga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaContext {

    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    public void put(String key, Object value) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(key, value);
    }

    public Object get(String key) {
        if (data == null) {
            return null;
        }
        return data.get(key);
    }

    public Long getLong(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public BigDecimal getBigDecimal(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public String getString(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}

