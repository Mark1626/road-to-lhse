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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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

        var expectedRecords = List.of(
                new RecfileRecord(Map.of("name", new RecfileField("name", "A"), "score", new RecfileField("score", "10"))),
                new RecfileRecord(Map.of("name", new RecfileField("name", "B"), "score", new RecfileField("score", "20")))
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

        var expectedRecords = List.of(
                new RecfileRecord(Map.of("name", new RecfileField("name", "A"), "score", new RecfileField("score", "10"))),
                new RecfileRecord(Map.of("name", new RecfileField("name", "B"), "score", new RecfileField("score", "20"), "grade", new RecfileField("grade", "A")))
        );
        var expectedFields = Set.of("name", "score", "grade");

        Assertions.assertEquals(expectedRecords, rec.getRecords());
        Assertions.assertEquals(expectedFields, rec.getFields());
    }
}
