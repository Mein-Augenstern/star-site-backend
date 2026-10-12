package com.xp.starsite.member.repository.impl;

import com.xp.starsite.common.async.AsyncCommand;
import com.xp.starsite.member.converter.AsyncCommandConverter;
import com.xp.starsite.member.dal.dataobject.AsyncCommandDO;
import com.xp.starsite.member.mapper.AsyncCommandAdminMapper;
import com.xp.starsite.member.repository.AsyncCommandAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 异步命令管控仓储实现：调用 dal 的 {@link AsyncCommandAdminMapper}（DAO）操作数据库，
 * DO↔AsyncCommand 转换收敛于此——上层（service）只见组件模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class AsyncCommandAdminRepositoryImpl implements AsyncCommandAdminRepository {

    private final AsyncCommandAdminMapper adminMapper;

    @Override
    public long countByStatus(String status) {
        return adminMapper.countByStatus(status);
    }

    @Override
    public long countBacklogDue(LocalDateTime now) {
        return adminMapper.countBacklogDue(now);
    }

    @Override
    public List<AsyncCommand> recent(int limit) {
        return adminMapper.selectRecent(limit)
                .stream().map(AsyncCommandConverter::toEntity).toList();
    }

    @Override
    public long countPage(String status, String cmdType) {
        return adminMapper.countPage(status, cmdType);
    }

    @Override
    public List<AsyncCommand> page(String status, String cmdType, long offset, long limit) {
        return adminMapper.selectPage(status, cmdType, offset, limit)
                .stream().map(AsyncCommandConverter::toEntity).toList();
    }

    @Override
    public Optional<AsyncCommand> findById(Long id) {
        return Optional.ofNullable(adminMapper.selectById(id)).map(AsyncCommandConverter::toEntity);
    }

    @Override
    public void updateState(AsyncCommand cmd) {
        adminMapper.updateState(AsyncCommandConverter.toDO(cmd));
    }
}
