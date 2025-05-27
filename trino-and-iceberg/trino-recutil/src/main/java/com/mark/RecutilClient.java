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
import com.google.inject.Inject;
import com.mark.jrecutil.JRecutil;
import com.mark.session.ISession;
import com.mark.session.SessionProvider;
import io.airlift.json.JsonCodec;
import io.trino.spi.type.VarcharType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class RecutilClient
{
    private final Logger logger = Logger.getLogger(RecutilClient.class.getName());
    private final RecutilConfig config;
    private final String suffix;

    @Inject
    public RecutilClient(RecutilConfig config, JsonCodec<Map<String, List<RecutilTable>>> catalogCodec)
    {
        requireNonNull(config, "config is null");
//        requireNonNull(catalogCodec, "catalogCodec is null");
        this.config = config;
        this.suffix = config.getSuffix() == null ? "rec" : config.getSuffix();
    }

    public List<String> getSchemaNames()
    {
        try {
            ISession session = getSession();
            List<String> schemas = session.getSchemas();
            session.close();
            return ImmutableList.copyOf(schemas);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getTableNames(String schemaName)
    {
        requireNonNull(schemaName, "schemaName is null");
        try {
            ISession session = getSession();
            List<String> schemas = session.getTables(schemaName, suffix);
            session.close();
            return ImmutableList.copyOf(schemas);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<RecutilTable> getTable(String schemaName, String tableName)
    {
        Set<String> fieldNames;
        try {
            var session = getSession();
            var inputStream = session.getInputStream(schemaName, tableName);

            var recfile = new JRecutil(inputStream);
            fieldNames = recfile.getFields();

            inputStream.close();
            session.close();
        }
        catch (Exception e) {
            logger.warning(String.format("Error while reading recfile %s", tableName));
            return Optional.empty();
        }

        // TODO: Add column typing
        var columns = fieldNames.stream()
                .map(field -> new RecutilColumnn(field, VarcharType.VARCHAR))
                .toList();

        return Optional.of(new RecutilTable(tableName, columns));
    }

    public ISession getSession()
    {
        Map<String, String> sessionInfo = new HashMap<>();
        sessionInfo.put("base", config.getBase());
        sessionInfo.put("protocol", config.getProtocol());
        sessionInfo.put("host", config.getHost());
        sessionInfo.put("port", config.getPort().toString());
        sessionInfo.put("username", config.getUsername());
        sessionInfo.put("password", config.getPassword());
        return new SessionProvider(sessionInfo).getSession();
    }

    public List<String> getTablenames(String schemaName)
    {
        requireNonNull(schemaName, "schemaName is null");
        try {
            var session = getSession();
            List<String> tables = session.getTables(schemaName, suffix);
            session.close();
            return ImmutableList.copyOf(tables);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
