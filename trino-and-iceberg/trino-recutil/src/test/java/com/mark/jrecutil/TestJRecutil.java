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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class TestJRecutil
{
    @Test
    public void testShouldBeAbleToParseRecfile()
            throws IOException
    {
        String example = """
        Name: A
        Score: 10

        Name: B
        Score: 20
        """;
        var inputStream = new ByteArrayInputStream(example.getBytes(StandardCharsets.UTF_8));

        JRecutil rec = new JRecutil(inputStream);

        Multimap<String, String> recordA = HashMultimap.create();
        recordA.put("name", "A");
        recordA.put("score", "10");

        Multimap<String, String> recordB = HashMultimap.create();
        recordB.put("name", "B");
        recordB.put("score", "20");

        var expectedRecords = List.of(
                new RecfileRecord(recordA),
                new RecfileRecord(recordB)
        );
        var expectedFields = Set.of("name", "score");

        Assertions.assertEquals(expectedRecords, rec.getRecords());
        Assertions.assertEquals(expectedFields, rec.getFields());
    }

    @Test
    public void testShouldBeAbleToParseRecfileWithDifferentNumberOfKeys()
            throws IOException
    {
        String example = """
        Name: A
        Score: 10

        Name: B
        Score: 20
        Grade: A
        """;
        var inputStream = new ByteArrayInputStream(example.getBytes(StandardCharsets.UTF_8));

        JRecutil rec = new JRecutil(inputStream);

        Multimap<String, String> recordA = HashMultimap.create();
        recordA.put("name", "A");
        recordA.put("score", "10");

        Multimap<String, String> recordB = HashMultimap.create();
        recordB.put("name", "B");
        recordB.put("score", "20");
        recordB.put("grade", "A");

        var expectedRecords = List.of(
                new RecfileRecord(recordA),
                new RecfileRecord(recordB)
        );
        var expectedFields = Set.of("name", "score", "grade");

        Assertions.assertEquals(expectedRecords, rec.getRecords());
        Assertions.assertEquals(expectedFields, rec.getFields());
    }

    @Test
    public void testShouldBeAbleToParseRecfileWithMultipleValuesForSameField()
            throws IOException {
        String example = """
                Name: A
                Score: 10
                Tag: A
                Tag: B

                Name: B
                Score: 20
                Grade: A
                """;
        var inputStream = new ByteArrayInputStream(example.getBytes(StandardCharsets.UTF_8));

        JRecutil rec = new JRecutil(inputStream);

        Multimap<String, String> recordA = HashMultimap.create();
        recordA.put("name", "A");
        recordA.put("score", "10");
        recordA.put("tag", "A");
        recordA.put("tag", "B");

        Multimap<String, String> recordB = HashMultimap.create();
        recordB.put("name", "B");
        recordB.put("score", "20");
        recordB.put("grade", "A");

        var expectedRecords = List.of(
                new RecfileRecord(recordA),
                new RecfileRecord(recordB)
        );
        var expectedFields = Set.of("name", "score", "grade", "tag");

        Assertions.assertEquals(expectedFields, rec.getFields());
        Assertions.assertEquals(expectedRecords, rec.getRecords());
    }
}
