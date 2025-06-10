# RKDB

A simple key-value store backed by sqlite in Rust that supports REdis Serialization Protocol. **Created for learning purposes, this is not a full fledged key value store**

> Note: I'm still not able to use the redis cli directly

## Building

```
cargo build
```

## Running RKDB

```bash
rkdb --host 127.0.0.1 --port 8080 --db-path ./rkdb.db

echo "*1\r\n$4\r\nPING\r\n" | nc -v 127.0.0.1 8080

echo "*3\r\n$3\r\nSET\r\n$4\r\nfoo5\r\n$3\r\nbar\r\n" | nc -v 127.0.0.1 8080

echo "*2\r\n$3\r\nGET\r\n$4\r\nfoo5\r\n" | nc -v 127.0.0.1 8080 
```

## Acknowledgements

Inspiration from [redka](https://github.com/nalgeon/redka.git) by [nalgeon](https://github.com/nalgeon).

## TODO

1. The `unwrap`'s are all over the place
2. Add default values for the cli arguments
