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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.trino.spi.connector.ColumnMetadata;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class RecutilTable
{
    private final List<ColumnMetadata> columnsMetadata;

    @JsonCreator
    public RecutilTable(
            @JsonProperty("name") String name,
            @JsonProperty("columns") List<RecutilColumnn> columns)
    {
        checkArgument(!isNullOrEmpty(name), "name is null or is empty");
        requireNonNull(columns, "columns is null");

        ImmutableList.Builder<ColumnMetadata> columnsMetadataBuilder = ImmutableList.builder();
        for (RecutilColumnn column : columns) {
            columnsMetadataBuilder.add(new ColumnMetadata(column.getName(), column.getType()));
        }

        this.columnsMetadata = columnsMetadataBuilder.build();
    }

    @JsonProperty
    public String getName()
    {
        return columnsMetadata.get(0).getName();
    }

    @JsonProperty
    public List<RecutilColumnn> getColumns()
    {
        ImmutableList.Builder<RecutilColumnn> columnBuilder = ImmutableList.builder();
        for (ColumnMetadata columnMetadata : columnsMetadata) {
            columnBuilder.add(new RecutilColumnn(columnMetadata.getName(), columnMetadata.getType()));
        }
        return columnBuilder.build();
    }

    public List<ColumnMetadata> getColumnsMetadata()
    {
        return columnsMetadata;
    }
}
