/*
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
package com.mark;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.ConnectorTableProperties;
import io.trino.spi.connector.ConnectorTableVersion;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.SchemaTablePrefix;
import io.trino.spi.connector.TableNotFoundException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

public class RecutilMetadata
        implements ConnectorMetadata
{
    private final RecutilClient recutilClient;
    private final String catalogName;

    @Inject
    public RecutilMetadata(@CatalogName String catalogName, RecutilClient recutilClient)
    {
        this.recutilClient = requireNonNull(recutilClient, "client is null");
        this.catalogName = requireNonNull(catalogName, "catalogName is null");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @BindingAnnotation
    public @interface CatalogName {}

    @Override
    public List<String> listSchemaNames(ConnectorSession session)
    {
        return recutilClient.getSchemaNames();
    }

    @Override
    public RecutilTableHandle getTableHandle(ConnectorSession session, SchemaTableName tableName, Optional<ConnectorTableVersion> startVersion, Optional<ConnectorTableVersion> endVersion)
    {
        requireNonNull(tableName, "tableName is null");
        if (!recutilClient.getSchemaNames().contains(tableName.getSchemaName())) {
            return null;
        }
        if (!recutilClient.getTableNames(tableName.getSchemaName()).contains(tableName.getTableName())) {
            return null;
        }
        return new RecutilTableHandle(tableName.getSchemaName(), tableName.getTableName());
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle table)
    {
        Optional<ConnectorTableMetadata> connectorTableMetadata = getTableMetadata(session, ((RecutilTableHandle) table).getSchemaTableName());
        if (connectorTableMetadata.isEmpty()) {
            throw new RuntimeException("Table not found: " + table);
        }
        return connectorTableMetadata.get();
    }

    private Optional<ConnectorTableMetadata> getTableMetadata(ConnectorSession session, SchemaTableName schemaTableName)
    {
        if (!listSchemaNames(session).contains(schemaTableName.getSchemaName())) {
            return Optional.empty();
        }
        Optional<RecutilTable> table = recutilClient.getTable(schemaTableName.getSchemaName(), schemaTableName.getTableName());
        return table.map(recTable -> new ConnectorTableMetadata(schemaTableName, recTable.getColumnsMetadata()));
    }

    @Override
    public Map<String, ColumnHandle> getColumnHandles(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
        RecutilTableHandle excelTableHandle = (RecutilTableHandle) tableHandle;
        Optional<RecutilTable> table = recutilClient.getTable(excelTableHandle.getSchemaName(), excelTableHandle.getTableName());
        if (table.isEmpty()) {
            throw new TableNotFoundException(excelTableHandle.getSchemaTableName());
        }
        ImmutableMap.Builder<String, ColumnHandle> columnHandles = ImmutableMap.builder();
        int i = 0;
        for (ColumnMetadata column : table.get().getColumnsMetadata()) {
            columnHandles.put(column.getName(), new RecutilColumnHandle(column.getName(), column.getType(), i++));
        }
        return columnHandles.build();
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle)
    {
        return ((RecutilColumnHandle) columnHandle).getColumnMetadata();
    }

    @Override
    public ConnectorTableProperties getTableProperties(ConnectorSession session, ConnectorTableHandle table)
    {
        return new ConnectorTableProperties();
    }

    @Override
    public Map<SchemaTableName, List<ColumnMetadata>> listTableColumns(ConnectorSession session, SchemaTablePrefix prefix)
    {
        requireNonNull(prefix, "prefix is null");
        ImmutableMap.Builder<SchemaTableName, List<ColumnMetadata>> columns = ImmutableMap.builder();
        for (SchemaTableName tableName : listTables(session, prefix.getSchema())) {
            Optional<ConnectorTableMetadata> tableMetadata = getTableMetadata(session, tableName);
            // table can disappear during listing operation
            tableMetadata.ifPresent(connectorTableMetadata -> columns.put(tableName, connectorTableMetadata.getColumns()));
        }
        return columns.build();
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> schemaName)
    {
        ImmutableList.Builder<String> schemaListBuilder = ImmutableList.builder();
        ImmutableList.Builder<SchemaTableName> tableListBuilder = ImmutableList.builder();
        if (schemaName.isPresent()) {
            schemaListBuilder.add(schemaName.get());
        }
        else {
            schemaListBuilder.addAll(listSchemaNames(session));
        }
        ImmutableList<String> schemaList = schemaListBuilder.build();
        for (String schema : schemaList) {
            tableListBuilder.addAll(listTables(schema));
        }
        return tableListBuilder.build();
    }

    private List<SchemaTableName> listTables(String schemaName)
    {
        return recutilClient.getTableNames(schemaName).stream()
                .map(tableName -> new SchemaTableName(schemaName, tableName))
                .collect(toImmutableList());
    }

//    public Optional<ConstraintApplicationResult<ConnectorTableHandle>> applyFilter(ConnectorSession session, ConnectorTableHandle table, Constraint constraint)
}
