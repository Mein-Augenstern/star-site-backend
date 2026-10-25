/*
 * Copyright 2026 muyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xp.starsite.common.util;

import com.xp.starsite.sequence.SnowflakeIdGenerator;
import com.xp.starsite.sequence.StarsiteIdGenerator;

/**
 * 雪花 id 生成器（兼容门面）：引擎已迁移至 starsite-sequence 组件
 * （com.xp.starsite:starsite-sequence），本类仅保留原 API 形态（nextId() 与
 * {@code new IdGenerator(workerId)} 构造）委托组件引擎——业务代码与测试零改动。
 * <p>
 * 使用约定：需要「insert 前拿到 id」的场景（事务消息事件载荷、跨服务引用）在业务层显式
 * {@code entity.setId(idGenerator.nextId())}；纯自增表不适用（DB 自增）。
 * 单例 Bean：由 StarSiteLogConfig 注册（分配策略编排见其注释）；测试场景可 {@code new IdGenerator(1)}。
 * 后续新代码建议直接注入组件的 {@link StarsiteIdGenerator}（next()/nextStr()）。
 *
 * @author muyi
 * @date 2026/10/25
 */
public class IdGenerator {

    /** 组件引擎门面（真正发号者；workerId 分配策略由构造方决定） */
    private final StarsiteIdGenerator delegate;

    /**
     * 固定机器位构造（测试与显式配置场景）：standalone 形态直连组件引擎。
     *
     * @param workerId 机器位（0~1023，10 位机器位；多实例部署时各不相同即可）
     */
    public IdGenerator(long workerId) {
        this(new StarsiteIdGenerator(
                new SnowflakeIdGenerator(workerId, 1767225600000L, 10, 12, 2000,
                        SnowflakeIdGenerator.BackwardPolicy.CLAMP),
                workerId, "standalone/fixed(" + workerId + ")"));
    }

    /**
     * 委托构造（生产装配用）：StarSiteLogConfig 按「显式配置 → sequence 表租约 → hash 回落」
     * 编排好分配策略后，把现成的组件门面传入。
     *
     * @param delegate 组件发号门面（机器位已分配完毕）
     */
    public IdGenerator(StarsiteIdGenerator delegate) {
        this.delegate = delegate;
    }

    /**
     * 生成下一个全局唯一 id（同一毫秒内序列递增，翻毫秒重置；时钟回拨按 CLAMP 策略钳制）。
     *
     * @return 64 位雪花 id
     * @throws com.xp.starsite.sequence.ClockBackwardException 理论不抛（CLAMP 策略钳制任意幅度回拨）；
     *         声明于此供调用方感知引擎语义
     */
    public long nextId() {
        return delegate.next();
    }
}
