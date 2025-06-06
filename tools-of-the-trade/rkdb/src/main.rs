mod cli;
mod server;
mod db;

#[tokio::main]
async fn main() {
    let args = cli::parse_args();

    let _ = server::start_server(args).await.inspect_err(|e| {
        eprintln!("Error {}", e);
    });
}
