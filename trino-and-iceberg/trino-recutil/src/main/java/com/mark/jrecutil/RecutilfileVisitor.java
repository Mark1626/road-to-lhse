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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RecutilfileVisitor
        extends RecfileBaseVisitor<Recfile>
{
    @Override public Recfile visitFile(RecfileParser.FileContext ctx)
    {
        List<RecfileRecord> records = new ArrayList<>();

        RecutilRecordVisitor recutilRecordVisitor = new RecutilRecordVisitor();
        for (RecfileParser.LineContext line : ctx.line()) {
            line.accept(recutilRecordVisitor).ifPresent(records::add);
        }
        var recfields = records
                .stream()
                .map(RecfileRecord::getKeys)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        return new Recfile(records, recfields);
    }
}
