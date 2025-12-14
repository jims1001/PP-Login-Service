package com.PPCloud.PP_Login_Service.core.workflow;

import java.time.Instant;
import java.util.*;

public final class FlowBag {

    private final Map<String, Object> bag;

    public FlowBag(Map<String, Object> bag) {
        this.bag = Objects.requireNonNull(bag, "bag");
    }

    /* ======================= 基础类型 ======================= */

    public void putStr(String k, String v) {
        if (v != null) bag.put(k, v);
    }

    public String getStr(String k) {
        Object o = bag.get(k);
        return (o instanceof String s) ? s : null;
    }

    public String requireStr(String k) {
        String v = getStr(k);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required string in FlowBag: " + k);
        }
        return v;
    }

    public void putBool(String k, boolean v) {
        bag.put(k, v);
    }

    public boolean getBool(String k) {
        Object o = bag.get(k);
        return o instanceof Boolean b && b;
    }

    public Boolean getBoolNullable(String k) {
        Object o = bag.get(k);
        return (o instanceof Boolean b) ? b : null;
    }

    public void putInt(String k, int v) {
        bag.put(k, v);
    }

    public Integer getInt(String k) {
        Object o = bag.get(k);
        return (o instanceof Integer i) ? i : null;
    }

    public int getIntOrDefault(String k, int def) {
        Integer v = getInt(k);
        return v != null ? v : def;
    }

    public void putLong(String k, long v) {
        bag.put(k, v);
    }

    public Long getLong(String k) {
        Object o = bag.get(k);
        return (o instanceof Long l) ? l : null;
    }

    public void putDouble(String k, double v) {
        bag.put(k, v);
    }

    public Double getDouble(String k) {
        Object o = bag.get(k);
        return (o instanceof Double d) ? d : null;
    }

    /* ======================= 时间类型 ======================= */

    /** 推荐统一存 epochSeconds，避免序列化坑 */
    public void putInstant(String k, Instant instant) {
        if (instant != null) {
            bag.put(k, instant.getEpochSecond());
        }
    }

    public Instant getInstant(String k) {
        Object o = bag.get(k);
        if (o instanceof Long l) {
            return Instant.ofEpochSecond(l);
        }
        if (o instanceof Integer i) {
            return Instant.ofEpochSecond(i.longValue());
        }
        return null;
    }

    /* ======================= 集合类型 ======================= */

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String k, Class<T> type) {
        Object o = bag.get(k);
        if (o instanceof List<?> list) {
            for (Object e : list) {
                if (e != null && !type.isInstance(e)) {
                    return List.of();
                }
            }
            return (List<T>) list;
        }
        return List.of();
    }

    public <T> void putList(String k, List<T> list) {
        if (list != null) bag.put(k, new ArrayList<>(list));
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(String k, Class<K> kt, Class<V> vt) {
        Object o = bag.get(k);
        if (o instanceof Map<?, ?> m) {
            for (var e : m.entrySet()) {
                if (!kt.isInstance(e.getKey()) || !vt.isInstance(e.getValue())) {
                    return Map.of();
                }
            }
            return (Map<K, V>) m;
        }
        return Map.of();
    }

    public <K, V> void putMap(String k, Map<K, V> map) {
        if (map != null) bag.put(k, new HashMap<>(map));
    }

    /* ======================= Optional / remove ======================= */

    public Optional<String> optStr(String k) {
        return Optional.ofNullable(getStr(k));
    }

    public Object remove(String k) {
        return bag.remove(k);
    }

    public String removeStr(String k) {
        Object o = bag.remove(k);
        return (o instanceof String s) ? s : null;
    }

    /* ======================= 高级：防误用 ======================= */

    /** 用于调试或最终 DONE 时返回 */
    public Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(bag);
    }
}