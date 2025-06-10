from pyiceberg.catalog import load_catalog
import pyarrow.parquet as pq

warehouse_path = "warehouse"
catalog = load_catalog(
    "taxicatalog",
    **{
        'type': 'sql',
        "uri": f"postgresql+psycopg2://postgres:example@localhost/postgres",
        "warehouse": f"s3://{warehouse_path}",
        "s3.endpoint": "http://127.0.0.1:9000",
        "py-io-impl": "pyiceberg.io.pyarrow.PyArrowFileIO",
        "s3.access-key-id": "admin",
        "s3.secret-access-key": "password",
        "s3.region": "us-east-1"
    },
)

df = pq.read_table("./yellow_tripdata_2023-01.parquet")

if not ("taxi",) in catalog.list_namespaces():
    catalog.create_namespace("taxi")

if not ('taxi', 'taxi_dataset') in catalog.list_tables("taxi"):
    table = catalog.create_table(
        identifier="taxi.taxi_dataset",
        schema=df.schema,
        location=f"s3://{warehouse_path}/taxi"
    )
    table.append(df)

    print(len(table.scan().to_arrow()))
