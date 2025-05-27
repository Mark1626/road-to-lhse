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

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class JRecutil
{
    private final InputStream inputStream;
    private final Recfile recfile;

    public JRecutil(InputStream inputStream)
            throws IOException
    {
        this.inputStream = inputStream;
        ANTLRInputStream input = new ANTLRInputStream(this.inputStream);
        RecfileLexer lexer = new RecfileLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RecfileParser parser = new RecfileParser(tokens);

        ParseTree tree = parser.file();
        RecutilfileVisitor eval = new RecutilfileVisitor();
        this.recfile = eval.visit(tree);
    }

    public List<RecfileRecord> getRecords()
    {
        return recfile.records();
    }

    public Set<String> getFields()
    {
        return recfile.fieldnames();
    }
}
