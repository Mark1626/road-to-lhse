# Trino


## Starting with Postgres with a PostgreSQL connector

```sql
CREATE CATALOG pgcatalog USING postgresql
WITH (
  "connection-url" = 'jdbc:postgresql://database:5432/postgres',
  "connection-user" = 'postgres',
  "connection-password" = 'example',
  "case-insensitive-name-matching" = 'true'
);

SHOW SCHEMAS IN pgcatalog;

DROP CATALOG pgcatalog;
```

Trying out the examples in https://github.com/trinodb/trino-the-definitive-guide

## A detour experiment

I'm going to use `py-iceberg` to create a Iceberg catalog with the NYC dataset, with Postgres as the catalog backend.

```shell
python iceberg-catalog-creation.py
```

Now I'm going to add this iceberg catalog in Trino.

```sql
CREATE CATALOG pgiceberg_default USING iceberg
WITH (
  "iceberg.catalog.type" = 'jdbc',
  "iceberg.jdbc-catalog.driver-class" = 'org.postgresql.Driver',
  "iceberg.jdbc-catalog.connection-url" = 'jdbc:postgresql://database:5432/postgres',
  "iceberg.jdbc-catalog.connection-user" = 'postgres',
  "iceberg.jdbc-catalog.connection-password" = 'example',
  "iceberg.jdbc-catalog.catalog-name" = 'default',
  "iceberg.jdbc-catalog.default-warehouse-dir" = '/tmp/warehouse'
);

show tables in pgiceberg_default.default;

select * from pgiceberg_default.default.taxi_dataset limit 5;
```

In an Intel Mac with Colima, I had issue having the data in `/tmp` https://github.com/abiosoft/colima/issues/267. Parking this experiment for now to try on Linux

## Creating an Iceberg catalog on the airline dataset

```sql
CREATE CATALOG pgiceberg_airline USING iceberg
WITH (
  "iceberg.catalog.type" = 'jdbc',
  "iceberg.jdbc-catalog.driver-class" = 'org.postgresql.Driver',
  "iceberg.jdbc-catalog.connection-url" = 'jdbc:postgresql://database:5432/postgres',
  "iceberg.jdbc-catalog.connection-user" = 'postgres',
  "iceberg.jdbc-catalog.connection-password" = 'example',
  "iceberg.jdbc-catalog.catalog-name" = 'airline',
  "iceberg.jdbc-catalog.default-warehouse-dir" = '/tmp/airlinewarehouse'
);
```

