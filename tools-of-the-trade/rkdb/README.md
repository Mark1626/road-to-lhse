# RKDB

A simple key-value store backed by sqlite in Rust ~~that supports REdis Serialization Protocol~~. **Created for learning purposes, this is not a full fledged key value store**

## Building

```
cargo build
```

## Running RKDB

I made a big blunnder by making the server handle HTTP when it should rather handle TCP requests

```bash
rkdb --host 127.0.0.1 --port 8080 --db-path ./rkdb.db

curl -X POST localhost:8080 -H 'Content-Type: text/plain' --data '*3\r\n$3\r\nSET\r\n$4\r\nfoo2\r\n$3\r\nbar\r\n'

curl -X POST localhost:8080 -H 'Content-Type: text/plain' --data '*2\r\n$3\r\nGET\r\n$4\r\nfoo2\r\n'
```

## Acknowledgements

Inspiration from [redka](https://github.com/nalgeon/redka.git) by [nalgeon](https://github.com/nalgeon).

## TODO

1. The `unwrap`'s are all over the place
2. Add default values for the cli arguments
