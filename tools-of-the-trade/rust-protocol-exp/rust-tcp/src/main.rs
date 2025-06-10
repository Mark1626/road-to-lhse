use tokio::net::{TcpListener, TcpStream};
use tokio::io::{AsyncBufReadExt, AsyncWriteExt, BufReader};

async fn handle_connection(mut stream: TcpStream) {

    let (reader, mut writer) = stream.split();
    let mut reader = BufReader::new(reader);
    let mut line = String::new();

    loop {
        let bytes_read = reader.read_line(&mut line).await.unwrap();
        if bytes_read == 0 {
            break;
        }
        // writer.write_all(format!("Echo {}\n", line.trim_end()).as_bytes()).await.unwrap();
        writer.write_all("+OK\r\n".as_bytes()).await.unwrap();
        line.clear();
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let listener = TcpListener::bind("127.0.0.1:8080").await.unwrap();

    loop {
        let (stream, _) = listener.accept().await?;
        tokio::spawn(handle_connection(stream));
    }
}
