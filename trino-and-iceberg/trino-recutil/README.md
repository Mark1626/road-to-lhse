# Trino recfile plugin

A trino connector for recfiles, a human readable database. To learn more about recfiles see https://www.gnu.org/software/recutils/ and https://gist.github.com/gmolveau/6be062d9b9005cf985cda98dabdf0baa

## Building

### Building the plugin

#### Recfile Parser

The ANTLR parser generation is not automated with the Maven plugin, so download and run the ANTLR jar before compilation

```
wget https://www.antlr.org/download/antlr-4.13.2-complete.jar
java -jar antlr-4.13.2-complete.jar -no-listener -visitor -package com.mark.jrecutil src/main/java/com/mark/jrecutil/Recfile.g4
mvn license:format
mvn package
```

## Usage

Create a catalog properties file `/etc/catalog/recfile.properties`. If you want to read files from local drives, use the following configuration

```in
connector.name=recutil
rec.protocol=file
rec.base=/path/to/dir
rec.suffix=rec
```

## TODO

1. Support joins
2. Support remote files
3. Fix and use the ANTLR maven plugin
4. Maybe make a custom cursor instead of an inmemory cursor
5. When a field is repeated it should represented as a 1-Many relationship

