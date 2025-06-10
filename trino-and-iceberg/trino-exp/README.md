# Trino

## Notes

1. Connecting to trino's predefined sources seem simple
2. I wanted to create a catalog with pyiceberg and open it from trino. pyiceberg supported postgres and `file://` as a storage for the warehouse data.
3. Creating a `file://` warehouse in pyiceberg and trying to open it in a containerized trino was trying because of trying to have the volume path in the same name as the path in the host. Colima added to the problem as it can only mount `$HOME` and not mount `/tmp`. I'm unable to query this catalog in an Intel Mac with Colima, as I have an issue having the data in `/tmp` https://github.com/abiosoft/colima/issues/267. Parking this experiment for now to try on Linux
4. When I tried to create a iceberg catalog in Trino with a `file://` warehouse, the catalog, schema was created. But I wasn't able to create a table. I suspect trino allows the warehouse to be `file://`
5. Going to use a containerized minio for the storage instead of having to go through the hassle with `file://`.

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

I'm going to use `py-iceberg` to create a Iceberg catalog with the NYC dataset, with Postgres as the catalog backend and Minio as the warehouse

```shell
wget https://d37ci6vzurychx.cloudfront.net/trip-data/yellow_tripdata_2023-01.parquet
python iceberg-catalog-creation.py
```

Now I'm going to add this iceberg catalog in Trino

```
connector.name=iceberg
iceberg.catalog.type=jdbc
iceberg.jdbc-catalog.catalog-name=taxicatalog
iceberg.jdbc-catalog.connection-password=example
iceberg.jdbc-catalog.connection-url=jdbc\:postgresql\://database\:5432/postgres
iceberg.jdbc-catalog.connection-user=postgres
iceberg.jdbc-catalog.default-warehouse-dir=s3://warehouse
iceberg.jdbc-catalog.driver-class=org.postgresql.Driver
fs.native-s3.enabled=true
s3.endpoint=http://storage:9000
s3.region=us-east-1
s3.path-style-access=true
s3.aws-access-key=admin
s3.aws-secret-key=password
```

```
show schemas in pgiceberg_taxi;
show tables in pgiceberg_taxi.taxi;

select * from pgiceberg_taxi.taxi.taxi_dataset limit 5;
```

## Creating an Iceberg catalog on the airline dataset

```
connector.name=iceberg
iceberg.catalog.type=jdbc
iceberg.jdbc-catalog.catalog-name=airline
iceberg.jdbc-catalog.connection-password=example
iceberg.jdbc-catalog.connection-url=jdbc\:postgresql\://database\:5432/postgres
iceberg.jdbc-catalog.connection-user=postgres
iceberg.jdbc-catalog.default-warehouse-dir=s3://warehouse
iceberg.jdbc-catalog.driver-class=org.postgresql.Driver
fs.native-s3.enabled=true
s3.endpoint=http://storage:9000
s3.region=us-east-1
s3.path-style-access=true
s3.aws-access-key=admin
s3.aws-secret-key=password
```

## Reference

1. https://github.com/wirelessr/trino-iceberg-playground
