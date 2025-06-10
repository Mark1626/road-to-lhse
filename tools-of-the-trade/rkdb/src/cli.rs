use clap::Parser;

#[derive(Parser, Debug)]
#[command(version, about, long_about=None)]
pub struct Args {
    #[arg(long)]
    pub host: String,

    #[arg(long)]
    pub port: u16,

    #[arg(long)]
    pub db_path: String,
}

pub fn parse_args() -> Args {
    Args::parse()
}
