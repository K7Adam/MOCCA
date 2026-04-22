use portable_pty::{native_pty_system, CommandBuilder, PtySize};
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::collections::{HashMap, HashSet};
use std::io::{self, BufRead, Read, Write};
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;
use uuid::Uuid;

#[derive(Debug, Deserialize)]
#[serde(tag = "cmd", rename_all = "camelCase")]
enum Command {
    Spawn { cols: Option<u16>, rows: Option<u16>, shell: Option<String> },
    Write { #[serde(rename = "terminalId")] terminal_id: String, data: String },
    Resize { #[serde(rename = "terminalId")] terminal_id: String, cols: u16, rows: u16 },
    Snapshot { #[serde(rename = "terminalId")] terminal_id: String },
    Kill { #[serde(rename = "terminalId")] terminal_id: String },
}

#[derive(Clone, Debug, Serialize)]
struct CellJson {
    #[serde(rename = "char")]
    ch: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    fg: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    bg: Option<String>,
    #[serde(skip_serializing_if = "Vec::is_empty", default)]
    attrs: Vec<String>,
}

struct Session {
    writer: Box<dyn Write + Send>,
    child: Box<dyn portable_pty::Child + Send + Sync>,
    shell: String,
    title: String,
    cols: u16,
    rows: u16,
    lines: Arc<Mutex<Vec<String>>>,
    dirty_rows: Arc<Mutex<HashSet<usize>>>,
}

fn main() {
    let stdout = Arc::new(Mutex::new(io::stdout()));
    let mut sessions: HashMap<String, Session> = HashMap::new();
    let stdin = io::stdin();

    for line in stdin.lock().lines() {
        let line = match line {
            Ok(line) => line,
            Err(error) => {
                emit(&stdout, json!({ "event": "error", "message": error.to_string() }));
                continue;
            }
        };

        let command: Command = match serde_json::from_str(&line) {
            Ok(command) => command,
            Err(error) => {
                emit(&stdout, json!({ "event": "error", "message": error.to_string() }));
                continue;
            }
        };

        match command {
            Command::Spawn { cols, rows, shell } => {
                match spawn_session(cols.unwrap_or(120), rows.unwrap_or(40), shell, stdout.clone()) {
                    Ok((terminal_id, session)) => {
                        emit(&stdout, json!({
                            "event": "spawned",
                            "terminalId": terminal_id,
                            "shell": session.shell,
                            "title": session.title,
                            "cols": session.cols,
                            "rows": session.rows
                        }));
                        sessions.insert(terminal_id, session);
                    }
                    Err(error) => emit(&stdout, json!({ "event": "error", "message": error.to_string() })),
                }
            }
            Command::Write { terminal_id, data } => {
                if let Some(session) = sessions.get_mut(&terminal_id) {
                    let _ = session.writer.write_all(data.as_bytes());
                    let _ = session.writer.flush();
                }
            }
            Command::Resize { terminal_id, cols, rows } => {
                if let Some(session) = sessions.get_mut(&terminal_id) {
                    session.cols = cols;
                    session.rows = rows;
                    if let Ok(mut dirty) = session.dirty_rows.lock() {
                        dirty.extend(0..rows as usize);
                    }
                    emit_state(&stdout, &terminal_id, session, true);
                }
            }
            Command::Snapshot { terminal_id } => {
                if let Some(session) = sessions.get(&terminal_id) {
                    emit_state(&stdout, &terminal_id, session, true);
                }
            }
            Command::Kill { terminal_id } => {
                if let Some(mut session) = sessions.remove(&terminal_id) {
                    let _ = session.child.kill();
                    let _ = session.child.wait();
                    emit(&stdout, json!({ "event": "exit", "terminalId": terminal_id, "killed": true }));
                }
            }
        }
    }
}

fn spawn_session(
    cols: u16,
    rows: u16,
    shell: Option<String>,
    stdout: Arc<Mutex<io::Stdout>>,
) -> io::Result<(String, Session)> {
    let pty_system = native_pty_system();
    let pair = pty_system
        .openpty(PtySize { rows, cols, pixel_width: 0, pixel_height: 0 })
        .map_err(|error| io::Error::new(io::ErrorKind::Other, error.to_string()))?;
    let shell = shell.unwrap_or_else(default_shell);
    let title = std::path::Path::new(&shell)
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or(&shell)
        .to_string();
    let cmd = CommandBuilder::new(&shell);
    let child = pair
        .slave
        .spawn_command(cmd)
        .map_err(|error| io::Error::new(io::ErrorKind::Other, error.to_string()))?;
    let mut reader = pair
        .master
        .try_clone_reader()
        .map_err(|error| io::Error::new(io::ErrorKind::Other, error.to_string()))?;
    let writer = pair
        .master
        .take_writer()
        .map_err(|error| io::Error::new(io::ErrorKind::Other, error.to_string()))?;
    let terminal_id = Uuid::new_v4().to_string();
    let lines = Arc::new(Mutex::new(vec![String::new()]));
    let dirty_rows = Arc::new(Mutex::new((0..rows as usize).collect::<HashSet<_>>()));
    let reader_lines = lines.clone();
    let reader_dirty = dirty_rows.clone();
    let reader_id = terminal_id.clone();
    let reader_stdout = stdout.clone();

    thread::spawn(move || {
        let mut buffer = [0_u8; 4096];
        loop {
            let read = match reader.read(&mut buffer) {
                Ok(0) => break,
                Ok(read) => read,
                Err(_) => break,
            };
            append_text(&reader_lines, &reader_dirty, rows as usize, &String::from_utf8_lossy(&buffer[..read]));
            thread::sleep(Duration::from_millis(42));
            emit_state_parts(&reader_stdout, &reader_id, cols, rows, &reader_lines, &reader_dirty, false);
        }
        emit(&reader_stdout, json!({ "event": "exit", "terminalId": reader_id }));
    });

    Ok((terminal_id, Session { writer, child, shell, title, cols, rows, lines, dirty_rows }))
}

fn append_text(lines: &Arc<Mutex<Vec<String>>>, dirty_rows: &Arc<Mutex<HashSet<usize>>>, rows: usize, text: &str) {
    if let Ok(mut lines) = lines.lock() {
        for ch in text.chars() {
            match ch {
                '\r' => {
                    if let Some(last) = lines.last_mut() {
                        last.clear();
                    }
                }
                '\n' => lines.push(String::new()),
                '\x08' => {
                    if let Some(last) = lines.last_mut() {
                        last.pop();
                    }
                }
                ch if !ch.is_control() => {
                    if let Some(last) = lines.last_mut() {
                        last.push(ch);
                    }
                }
                _ => {}
            }
        }
        if lines.len() > 1000 {
            let keep_from = lines.len() - 1000;
            lines.drain(0..keep_from);
        }
    }
    if let Ok(mut dirty) = dirty_rows.lock() {
        dirty.extend(0..rows);
    }
}

fn emit_state(stdout: &Arc<Mutex<io::Stdout>>, terminal_id: &str, session: &Session, full_frame: bool) {
    emit_state_parts(stdout, terminal_id, session.cols, session.rows, &session.lines, &session.dirty_rows, full_frame);
}

fn emit_state_parts(
    stdout: &Arc<Mutex<io::Stdout>>,
    terminal_id: &str,
    cols: u16,
    rows: u16,
    lines: &Arc<Mutex<Vec<String>>>,
    dirty_rows: &Arc<Mutex<HashSet<usize>>>,
    full_frame: bool,
) {
    let visible = {
        let lines = lines.lock().unwrap();
        let mut visible = lines.iter().rev().take(rows as usize).cloned().collect::<Vec<_>>();
        visible.reverse();
        while visible.len() < rows as usize {
            visible.insert(0, String::new());
        }
        visible
    };
    let row_indexes = if full_frame {
        (0..rows as usize).collect::<Vec<_>>()
    } else {
        let mut dirty = dirty_rows.lock().unwrap();
        let rows = dirty.iter().copied().collect::<Vec<_>>();
        dirty.clear();
        rows
    };
    let mut cells = serde_json::Map::new();
    for row in row_indexes {
        let line = visible.get(row).cloned().unwrap_or_default();
        let row_cells = line
            .chars()
            .take(cols as usize)
            .map(|ch| CellJson { ch: ch.to_string(), fg: None, bg: None, attrs: vec![] })
            .collect::<Vec<_>>();
        cells.insert(row.to_string(), serde_json::to_value(row_cells).unwrap());
    }
    emit(stdout, json!({
        "event": "state",
        "terminalId": terminal_id,
        "fullFrame": full_frame,
        "cols": cols,
        "rows": rows,
        "cells": cells,
        "cursorX": visible.last().map(|line| line.len()).unwrap_or(0).min(cols as usize),
        "cursorY": rows.saturating_sub(1),
        "cursorVisible": true,
        "cursorStyle": "block",
        "scrollbackLength": visible.len()
    }));
}

fn emit(stdout: &Arc<Mutex<io::Stdout>>, value: serde_json::Value) {
    if let Ok(mut stdout) = stdout.lock() {
        let _ = writeln!(stdout, "{}", value);
        let _ = stdout.flush();
    }
}

fn default_shell() -> String {
    if cfg!(windows) {
        std::env::var("COMSPEC").unwrap_or_else(|_| "powershell.exe".to_string())
    } else {
        std::env::var("SHELL").unwrap_or_else(|_| "/bin/sh".to_string())
    }
}
