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
package com.mark.jrecutil;

import com.mark.RecutilClient;
import com.mark.RecutilColumnHandle;
import com.mark.RecutilConfig;
import com.mark.RecutilConnector;
import com.mark.RecutilRecordSetProvider;
import com.mark.RecutilSplit;
import com.mark.RecutilTableHandle;
import io.trino.spi.type.VarcharType;
import io.trino.testing.TestingConnectorSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestRecutilRecordSetProvider
{
    @Test
    public void shouldTestRecordSetCreation()
    {
        RecutilConfig config = new RecutilConfig();
        config.setProtocol("file");
        config.setBase("examples");
        config.setPort(8080);

        RecutilClient client = new RecutilClient(config, null);
        RecutilRecordSetProvider recordSetProvider = new RecutilRecordSetProvider(client);

        var recordset = recordSetProvider.getRecordSet(
                RecutilConnector.RecutilTransactionHandle.INSTANCE,
                TestingConnectorSession.SESSION,
                new RecutilSplit("score", "test.rec"),
                new RecutilTableHandle("score", "test.rec"),
                List.of(
                        new RecutilColumnHandle("name", VarcharType.VARCHAR, 0),
                        new RecutilColumnHandle("score", VarcharType.VARCHAR, 1),
                        new RecutilColumnHandle("grade", VarcharType.VARCHAR, 2)));

        var cursor = recordset.cursor();
        List<RecfileRecord> records = new ArrayList<>();
        while (cursor.advanceNextPosition()) {
            Map<String, RecfileField> record = new HashMap<>();
            record.put("name", new RecfileField("name", cursor.getSlice(0).toStringUtf8()));
            record.put("score", new RecfileField("score", cursor.getSlice(1).toStringUtf8()));
            record.put("grade", new RecfileField("grade", cursor.getSlice(2).toStringUtf8()));
            records.add(new RecfileRecord(record));
        }

        Assertions.assertEquals(records, List.of(
                new RecfileRecord(Map.of("name", new RecfileField("name", "A"), "score", new RecfileField("score", "10"), "grade", new RecfileField("grade", ""))),
                new RecfileRecord(Map.of("name", new RecfileField("name", "B"), "score", new RecfileField("score", "20"), "grade", new RecfileField("grade", "A")))));
    }
}
