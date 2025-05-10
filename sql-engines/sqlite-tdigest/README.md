# sqlite tdigest

A t-digest aggregate function extension to sqlite

## Usage

```bash
make
sqlite test.db
```

In the SQL shell

```sql
.load tdigest
create table test (val float);
select count(val), tdigest(val, 0.5) from test;
select count(val), tdigest(val, 0.5) from test group by val < 5;
```
