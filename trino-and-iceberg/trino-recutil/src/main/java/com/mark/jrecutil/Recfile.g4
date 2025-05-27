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

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

grammar Recfile;

file
    : line* EOF
    ;

line
    : (descriptor | record)
    ;

descriptor
    : ('%' field FIELDSEP)+ eol
    ;

record
    : (field FIELDSEP)+ eol
    ;


field
    : key ':' (FIELDSEP '+')? value
    ;

key
    : STRING
    ;

value
    : STRING
    ;

eol
    : FIELDSEP*
    ;

STRING
    : [a-zA-Z0-9'.] [a-zA-Z0-9_ '.]*
    ;

FIELDSEP
    : [\r\n]
    ;

COMMENT
    : '#' ~ [\r\n]+ -> skip
    ;

WS
    : [ \t]+ -> skip
    ;