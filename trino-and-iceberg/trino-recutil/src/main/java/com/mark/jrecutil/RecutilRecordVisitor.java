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

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class RecutilRecordVisitor
        extends RecfileBaseVisitor<Optional<RecfileRecord>>
{
    @Override
    public Optional<RecfileRecord> visitDescriptor(RecfileParser.DescriptorContext ctx)
    {
        return Optional.empty();
    }

    @Override
    public Optional<RecfileRecord> visitRecord(RecfileParser.RecordContext ctx)
    {
        var fields = ctx.field()
                .stream()
                .map(field -> {
                    String fieldName = field.key().getText().toLowerCase(Locale.ENGLISH);
                    String fieldValue = field.value().getText();
                    return new RecfileField(fieldName, fieldValue);
                }).collect(Collectors.toMap(RecfileField::key, element -> element));
        return Optional.of(new RecfileRecord(fields));
    }
}
