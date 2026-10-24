package com.xp.starsite.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雪花 ID 生成器单元测试：不同机器位实例间不撞号、同实例高并发单调唯一。
 *
 * @author muyi
 * @since 2026-10
 */
class IdGeneratorTest {

    @Test
    void differentWorkerIdsNeverCollide() {
        IdGenerator g1 = new IdGenerator(0);
        IdGenerator g2 = new IdGenerator(1);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(g1.nextId());
            ids.add(g2.nextId());
        }
        assertEquals(20_000, ids.size());
    }

    @Test
    void sameWorkerIdMonotonicUnique() {
        IdGenerator generator = new IdGenerator(1);
        long prev = -1;
        for (int i = 0; i < 5_000; i++) {
            long id = generator.nextId();
            assertTrue(id > prev, "同实例 id 必须严格递增");
            prev = id;
        }
    }
}
