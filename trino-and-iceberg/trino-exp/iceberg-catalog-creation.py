from pyiceberg.catalog import load_catalog
import pyarrow.parquet as pq

warehouse_path = "/tmp/warehouse"
catalog = load_catalog(
    "default",
    **{
        'type': 'sql',
        "uri": f"postgresql+psycopg2://postgres:example@localhost/postgres",
        "warehouse": f"file://{warehouse_path}",
    },
)

df = pq.read_table("/tmp/yellow_tripdata_2023-01.parquet")

catalog.create_namespace("default")

table = catalog.create_table(
    "default.taxi_dataset",
    schema=df.schema,
)
table.append(df)

print(len(table.scan().to_arrow()))

