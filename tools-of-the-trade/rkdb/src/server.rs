use std::net::{Ipv4Addr, SocketAddrV4};
use hyper::{
    body::{Bytes, Incoming}, server::conn::http1, Method, Request, Response, StatusCode
};
use http_body_util::{BodyExt, Full};
use hyper_util::rt::TokioIo;
use tokio::net::TcpListener;
use hyper::service::service_fn;
use std::sync::{Arc, Mutex};

use crate::cli;
use crate::db::RKDBStore;

type SharedDB = Arc<Mutex<RKDBStore>>;

async fn handle_request_command(
    req: Request<Incoming>,
    db: SharedDB
) -> Result<Response<Full<Bytes>>, hyper::Error> {
    if req.method() != Method::POST {
        return Ok(Response::builder()
            .status(StatusCode::METHOD_NOT_ALLOWED)
            .body(Full::new(Bytes::from("-ERR only POST allowed\r\n")))
            .unwrap());
    }

    let body_bytes = req.collect().await?.to_bytes();
    let body_str = String::from_utf8(body_bytes.to_vec()).unwrap_or_default();

    let parts: Vec<&str> = body_str.split("\\r\\n").collect();
    if parts.len() < 3 {
        return Ok(Response::builder()
            .status(StatusCode::BAD_REQUEST)
            .body(Full::new(Bytes::from("-ERR invalid command format\r\n")))
            .unwrap());
    }

    let command = parts[2].to_uppercase();
    let args: Vec<&&str> = parts[4..].iter()
        .step_by(2)
        .filter(|s| !s.is_empty() && !s.starts_with('$'))
        .collect();

    let db_lock = db.lock().unwrap();
    let response = match command.as_str() {
        "SET" if args.len() >= 2 => {
            match db_lock.set(args[0], args[1]) {
                Ok(_) => "+OK\r\n".to_string(),
                Err(e) => format!("-ERR DB error {}\r\n", e)
            }
        }
        "GET" if !args.is_empty() => {
            match db_lock.get(args[0]) {
                Ok(Some(value)) => format!("${}\r\n{}\r\n", value.len(), value),
                Ok(None) => "$-1\r\n".to_string(),
                Err(e) => format!("-ERR DB error {}\r\n", e)
            }
        }
        // Skipping the DEL command
        // "DEL" if !args.is_empty() => {
        //     "".to_string()
        // }
        _ => "-ERR unknown command or wrong number of arguments\r\n".to_string()
    };

    Ok(Response::new(Full::new(Bytes::from(response))))
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
        let io = TokioIo::new(stream);
        let db = db.clone();

        tokio::spawn(async move {
            let svc = service_fn(move |req| {
                println!("Req {:?}", req);
                handle_request_command(req, db.clone())
            });
            if let Err(err) = http1::Builder::new().serve_connection(io, svc).await {
                eprintln!("server error: {}", err);
            }
        });
    }
}
