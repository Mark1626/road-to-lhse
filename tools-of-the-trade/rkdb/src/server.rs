use std::io::Error;
use std::net::{Ipv4Addr, SocketAddrV4};
use std::sync::Arc;
use tokio::io::{AsyncReadExt, AsyncWriteExt, BufReader};
use tokio::net::{TcpListener, TcpStream};
use tokio::sync::Mutex;

use crate::cli;
use crate::db::RKDBStore;

type SharedDB = Arc<Mutex<RKDBStore>>;

async fn handle_connection(mut stream: TcpStream, db: SharedDB) -> Result<(), Error> {
    let (reader, mut writer) = stream.split();
    let mut reader = BufReader::new(reader);
    let mut buffer = String::new();
    let bytes_read = reader.read_to_string(&mut buffer).await?;
    if bytes_read == 0 {
        return Ok(());
    }
    println!("Request: {}", buffer);

    let lines: Vec<&str> = buffer.split("\r\n").filter(|s| !s.is_empty()).collect();
    if lines.is_empty() {
        return Ok(writer.write_all("-ERR empty input\r\n".as_bytes()).await?);
    }

    if !lines[0].starts_with('*') {
        return Ok(writer
            .write_all("-ERR invalid RESP format\r\n".as_bytes())
            .await?);
    }

    let array_size: usize = match lines[0][1..].parse() {
        Ok(size) => size,
        Err(_) => {
            return Ok(writer
                .write_all("-ERR invalid array size\r\n".as_bytes())
                .await?);
        }
    };

    if array_size == 0 {
        return Ok(writer
            .write_all("-ERR empty command array\r\n".as_bytes())
            .await?);
    }

    let command = lines[1].to_uppercase();
    let args: Vec<&&str> = lines[2..]
        .iter()
        .filter(|l| !l.starts_with('$') && !l.starts_with('*'))
        .collect();

    let db_lock = db.lock().await;
    let response = match command.as_str() {
        "SET" if args.len() >= 2 => match db_lock.set(&args[0], &args[1]) {
            Ok(_) => "+OK\r\n".to_string(),
            Err(e) => format!("-ERR DB error {}\r\n", e),
        },
        "GET" if !args.is_empty() => match db_lock.get(&args[0]) {
            Ok(Some(value)) => format!("${}\r\n{}\r\n", value.len(), value),
            Ok(None) => "$-1\r\n".to_string(),
            Err(e) => format!("-ERR DB error {}\r\n", e),
        },
        "PING" => "+PONG\r\n".to_string(),
        "COMMAND" => "*2\r\n$3\r\nSET\r\n$3\r\nGET\r\n".to_string(),
        _ => format!("-ERR unknown command '{}'\r\n", command),
    };

    println!("Sending response: {}", response.replace("\r\n", "\\r\\n"));
    Ok(stream.write_all(response.as_bytes()).await?)
}

pub async fn start_server(args: cli::Args) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let port = args.port;
    let ip: Ipv4Addr = args.host.parse().expect("Invalid IPv4 address");

    let addr = SocketAddrV4::new(ip, port);
    let listener = TcpListener::bind(addr).await?;

    let rkdb_store = RKDBStore::new(&args.db_path).unwrap();
    let db = Arc::new(Mutex::new(rkdb_store));

    println!("Starting server on {:?}", addr);
    loop {
        let (stream, _) = listener.accept().await?;
        let db = db.clone();

        tokio::spawn(async move {
            if let Err(e) = handle_connection(stream, db).await {
                eprintln!("Error handling request {}", e)
            }
        });
    }
}
