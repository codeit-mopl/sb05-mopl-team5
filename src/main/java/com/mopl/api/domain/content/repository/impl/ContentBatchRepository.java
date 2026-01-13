package com.mopl.api.domain.content.repository.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public void batchUpdateWatcherCounts(Map<UUID, Long> updateMap) {
        String sql = "UPDATE contents SET watcher_count = ? WHERE id = UNHEX(REPLACE(?, '-', ''))";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            List<UUID> ids = new ArrayList<>(updateMap.keySet());

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                UUID id = ids.get(i);
                ps.setLong(1, updateMap.get(id));
                ps.setString(2, id.toString());
            }

            @Override
            public int getBatchSize() {
                return ids.size();
            }
        });
    }
}