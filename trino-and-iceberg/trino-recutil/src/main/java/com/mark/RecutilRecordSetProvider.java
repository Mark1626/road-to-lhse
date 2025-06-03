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

import com.google.inject.Inject;
import com.mark.jrecutil.JRecutil;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorRecordSetProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.InMemoryRecordSet;
import io.trino.spi.connector.RecordSet;

import java.util.List;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class RecutilRecordSetProvider
        implements ConnectorRecordSetProvider
{
    private static final Logger logger = Logger.getLogger(RecutilRecordSetProvider.class.getName());
    private final RecutilClient recutilClient;

    @Inject
    public RecutilRecordSetProvider(RecutilClient client)
    {
        this.recutilClient = requireNonNull(client, "recutilClient is null");
    }

    @Override
    public RecordSet getRecordSet(
            ConnectorTransactionHandle transaction,
            ConnectorSession session,
            ConnectorSplit split,
            ConnectorTableHandle table,
            List<? extends ColumnHandle> columns)
    {
        requireNonNull(split, "split is null");
        RecutilSplit recutilSplit = (RecutilSplit) split;
        var handles = columns.stream().map(c -> (RecutilColumnHandle) c).toList();
        var iSession = recutilClient.getSession();
        try {
            var inputStream = iSession.getInputStream(recutilSplit.getSchemaName(), recutilSplit.getTableName());

            var recfile = new JRecutil(inputStream);
            var mappedRows = recfile.getRecords()
                    .stream()
                    .map(record -> handles.stream()
                            .map(
                                    handle -> {
                                        var col = record.getColumn(handle.getColumnName());
                                        // TODO: This may not work everytime when types are introduced
                                        return String.join(",", col);
                                    })
                            .toList()
                    ).toList();

            var mappedTypes = handles.stream().map(RecutilColumnHandle::getColumnType).toList();
            return new InMemoryRecordSet(mappedTypes, mappedRows);
        }
        catch (Exception e) {
            logger.warning(String.format("Error reading recfile %s\n %s", recutilSplit.getTableName(), e.getMessage()));
        }

        return null;
    }
}
