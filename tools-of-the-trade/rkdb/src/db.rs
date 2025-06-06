use sqlite::{Connection, Error, State};
use std::time::{SystemTime, UNIX_EPOCH};

pub struct RKDBStore {
    conn: Connection,
}

const SCHEMA: &str = "
    create table if not exists
    rstring (
        key    text not null,
        value  text not null,

        etime    integer,
        mtime    integer not null
    ) strict;

    create unique index if not exists
    rstring_key_idx on rstring (key);

    create index if not exists
    rstring_etime_idx on rstring (etime)
    where etime is not null;
";

// Expiration time
const EXP_TIME: i32 = 5 * 60;

const SQLGET: &str = "
    select value
    from rstring
    where key = :key and (etime is null or etime > :etime)
";

const SQLSET: &str = "
    insert into rstring (key, value, etime, mtime)
    values (:key, :value, :etime, :mtime)
    on conflict (key) do update
    set value = excluded.value
";

impl RKDBStore {
    pub fn new(path: &str) -> Result<Self, Error> {
        let conn = Connection::open(path)?;
        conn.execute(SCHEMA)?;
        Ok(RKDBStore { conn })
    }

    pub fn set(&self, key: &str, val: &str) -> Result<(), Error> {
        let mtime = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs() as i32;

        let etime = mtime + EXP_TIME;

        let mut stmt = self.conn.prepare(SQLSET)?;
        let _ = stmt.bind(&[
                (":key", key),
                (":value", val),
                (":etime", &etime.to_string()),
                (":mtime", &mtime.to_string()),
            ][..]);
        let _ = stmt.next();
        Ok(())
    }

    pub fn get(&self, key: &str) -> Result<Option<String>, Error> {
        let etime = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs() as i32;

        let mut stmt= self.conn.prepare(SQLGET)?;
        
        stmt.bind(&[
            (":key", key),
            (":etime", &etime.to_string()),
        ][..])?;
        
        let mut rows = Vec::new();
        while let Ok(State::Row) = stmt.next() {
            rows.push(stmt.read::<String, _>("value")?);
        }

        if rows.len() > 0 {
            Ok(Some(rows[0].clone()))
        } else {
            Ok(None)
        }
    }
}
