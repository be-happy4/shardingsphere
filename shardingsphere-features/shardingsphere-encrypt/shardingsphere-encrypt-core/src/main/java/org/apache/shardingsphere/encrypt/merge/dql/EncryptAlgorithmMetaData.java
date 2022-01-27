/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.encrypt.merge.dql;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.encrypt.rule.EncryptRule;
import org.apache.shardingsphere.encrypt.spi.EncryptAlgorithm;
import org.apache.shardingsphere.infra.binder.segment.select.projection.Projection;
import org.apache.shardingsphere.infra.binder.segment.select.projection.impl.ColumnProjection;
import org.apache.shardingsphere.infra.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.metadata.schema.ShardingSphereSchema;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Encrypt algorithm meta data.
 */
@RequiredArgsConstructor
public final class EncryptAlgorithmMetaData {
    
    private final String schemaName;
    
    private final ShardingSphereSchema schema;
    
    private final EncryptRule encryptRule;
    
    private final SelectStatementContext selectStatementContext;
    
    /**
     * Find encryptor.
     *
     * @param columnIndex column index
     * @return encryptor
     */
    public Optional<EncryptAlgorithm> findEncryptor(final int columnIndex) {
        Optional<ColumnProjection> columnProjection = findColumnProjection(columnIndex);
        if (!columnProjection.isPresent()) {
            return Optional.empty();
        }
        Map<String, String> columnTableNames = selectStatementContext.getTablesContext().findTableName(Collections.singletonList(columnProjection.get()), schema);
        Optional<String> tableName = Optional.ofNullable(columnTableNames.get(columnProjection.get().getExpression()));
        String columnName = columnProjection.get().getName();
        return tableName.isPresent() ? findEncryptor(schemaName, tableName.get(), columnName) : findEncryptor(schemaName, columnName);
    }
    
    private Optional<EncryptAlgorithm> findEncryptor(final String schemaName, final String tableName, final String columnName) {
        return encryptRule.findEncryptor(schemaName, tableName, columnName);
    }
    
    private Optional<EncryptAlgorithm> findEncryptor(final String schemaName, final String columnName) {
        for (String each : selectStatementContext.getTablesContext().getTableNames()) {
            Optional<EncryptAlgorithm> result = encryptRule.findEncryptor(schemaName, each, columnName);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
    
    /**
     * Judge whether table is support QueryWithCipherColumn or not.
     *
     * @param columnIndex column index
     * @return whether table is support QueryWithCipherColumn or not
     */
    public boolean isQueryWithCipherColumn(final int columnIndex) {
        Optional<ColumnProjection> columnProjection = findColumnProjection(columnIndex);
        Optional<String> tableName = Optional.empty();
        if (columnProjection.isPresent()) {
            Map<String, String> columnTableNames = selectStatementContext.getTablesContext().findTableName(Collections.singletonList(columnProjection.get()), schema);
            tableName = Optional.ofNullable(columnTableNames.get(columnProjection.get().getExpression()));
        }
        return encryptRule.isQueryWithCipherColumn(tableName.orElse(""));

    }
    
    private Optional<ColumnProjection> findColumnProjection(final int columnIndex) {
        List<Projection> expandProjections = selectStatementContext.getProjectionsContext().getExpandProjections();
        if (expandProjections.size() < columnIndex) {
            return Optional.empty();
        }
        Projection projection = expandProjections.get(columnIndex - 1);
        return projection instanceof ColumnProjection ? Optional.of((ColumnProjection) projection) : Optional.empty();
    }
}
