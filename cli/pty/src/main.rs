use portable_pty::{native_pty_system, CommandBuilder, MasterPty, PtySize};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::collections::{HashMap, HashSet};
use std::io::{self, BufRead, Read, Write};
use std::path::Path;
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;
use uuid::Uuid;

const DEFAULT_SCROLLBACK: usize = 1_000;
const FRAME_INTERVAL_MS: u64 = 42;

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

struct TerminalState {
    parser: vt100::Parser,
    version: u64,
}

struct Session {
    master: Box<dyn MasterPty + Send>,
    writer: Arc<Mutex<Box<dyn Write + Send>>>,
    child: Box<dyn portable_pty::Child + Send + Sync>,
    shell: String,
    title: String,
    cols: u16,
    rows: u16,
    state: Arc<Mutex<TerminalState>>,
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
                    let input = normalize_terminal_input(&data);
                    if let Ok(mut writer) = session.writer.lock() {
                        let _ = writer.write_all(input.as_bytes());
                        let _ = writer.flush();
                    }
                } else {
                    emit(&stdout, json!({
                        "event": "error",
                        "terminalId": terminal_id,
                        "message": "Terminal session was not found"
                    }));
                }
            }
            Command::Resize { terminal_id, cols, rows } => {
                if let Some(session) = sessions.get_mut(&terminal_id) {
                    session.cols = cols;
                    session.rows = rows;
                    let _ = session.master.resize(PtySize {
                        rows,
                        cols,
                        pixel_width: 0,
                        pixel_height: 0,
                    });
                    if let Ok(mut state) = session.state.lock() {
                        state.parser.screen_mut().set_size(rows, cols);
                        state.version = state.version.saturating_add(1);
                    }
                    mark_all_rows(&session.dirty_rows, rows);
                    emit_state(&stdout, &terminal_id, session, true);
                } else {
                    emit(&stdout, json!({
                        "event": "error",
                        "terminalId": terminal_id,
                        "message": "Terminal session was not found"
                    }));
                }
            }
            Command::Snapshot { terminal_id } => {
                if let Some(session) = sessions.get(&terminal_id) {
                    emit_state(&stdout, &terminal_id, session, true);
                } else {
                    emit(&stdout, json!({
                        "event": "error",
                        "terminalId": terminal_id,
                        "message": "Terminal session was not found"
                    }));
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
    let title = Path::new(&shell)
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or(&shell)
        .to_string();
    let cmd = build_shell_command(&shell, cols, rows);
    let child = pair
        .slave
        .spawn_command(cmd)
        .map_err(|error| io::Error::new(io::ErrorKind::Other, error.to_string()))?;
    let mut reader = pair
        .master
        .try_clone_reader()
        .map_err(|error| io::Error::new(io::ErrorKind::Other, error.to_string()))?;
    let writer = Arc::new(Mutex::new(pair
        .master
        .take_writer()
        .map_err(|error| io::Error::new(io::ErrorKind::Other, error.to_string()))?));
    let master = pair.master;
    let terminal_id = Uuid::new_v4().to_string();
    let state = Arc::new(Mutex::new(TerminalState {
        parser: vt100::Parser::new(rows, cols, DEFAULT_SCROLLBACK),
        version: 0,
    }));
    let dirty_rows = Arc::new(Mutex::new((0..rows as usize).collect::<HashSet<_>>()));
    let reader_state = state.clone();
    let reader_dirty = dirty_rows.clone();
    let reader_id = terminal_id.clone();
    let reader_stdout = stdout.clone();
    let reader_writer = writer.clone();

    thread::spawn(move || {
        let mut buffer = [0_u8; 4096];
        let debug_raw = std::env::var("MOCCA_PTY_DEBUG").is_ok_and(|value| value == "1" || value.eq_ignore_ascii_case("true"));
        loop {
            let read = match reader.read(&mut buffer) {
                Ok(0) => break,
                Ok(read) => read,
                Err(_) => break,
            };
            if debug_raw {
                eprintln!("pty read {read} bytes: {}", String::from_utf8_lossy(&buffer[..read]).escape_debug());
            }
            let responses = if let Ok(mut state) = reader_state.lock() {
                state.parser.process(&buffer[..read]);
                state.version = state.version.saturating_add(1);
                terminal_query_responses(&buffer[..read], state.parser.screen().cursor_position())
            } else {
                Vec::new()
            };
            for response in responses {
                if let Ok(mut writer) = reader_writer.lock() {
                    let _ = writer.write_all(response.as_bytes());
                    let _ = writer.flush();
                }
            }
            mark_all_rows(&reader_dirty, rows);
            thread::sleep(Duration::from_millis(FRAME_INTERVAL_MS));
            emit_state_parts(&reader_stdout, &reader_id, cols, rows, &reader_state, &reader_dirty, false);
        }
        emit(&reader_stdout, json!({ "event": "exit", "terminalId": reader_id }));
    });

    Ok((terminal_id, Session { master, writer, child, shell, title, cols, rows, state, dirty_rows }))
}

fn build_shell_command(shell: &str, cols: u16, rows: u16) -> CommandBuilder {
    let mut cmd = CommandBuilder::new(shell);
    if let Ok(cwd) = std::env::current_dir() {
        cmd.cwd(cwd.as_os_str());
    }
    cmd.env("TERM", std::env::var("TERM").unwrap_or_else(|_| "xterm-256color".to_string()));
    cmd.env("COLORTERM", std::env::var("COLORTERM").unwrap_or_else(|_| "truecolor".to_string()));
    cmd.env("COLUMNS", cols.to_string());
    cmd.env("LINES", rows.to_string());

    let shell_name = Path::new(shell)
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or(shell)
        .to_ascii_lowercase();
    if cfg!(windows) {
        if shell_name == "cmd.exe" || shell_name == "cmd" {
            cmd.arg("/K");
        } else if shell_name == "powershell.exe"
            || shell_name == "powershell"
            || shell_name == "pwsh.exe"
            || shell_name == "pwsh"
        {
            cmd.arg("-NoLogo");
            cmd.arg("-NoExit");
        }
    } else if shell_name == "sh"
        || shell_name == "bash"
        || shell_name == "zsh"
        || shell_name == "fish"
    {
        cmd.arg("-i");
    }
    cmd
}

fn mark_all_rows(dirty_rows: &Arc<Mutex<HashSet<usize>>>, rows: u16) {
    if let Ok(mut dirty) = dirty_rows.lock() {
        dirty.extend(0..rows as usize);
    }
}

fn normalize_terminal_input(data: &str) -> String {
    data.replace("\r\n", "\n").replace('\r', "\n").replace('\n', "\r\n")
}

fn terminal_query_responses(bytes: &[u8], cursor_position: (u16, u16)) -> Vec<String> {
    let mut responses = Vec::new();
    if contains_sequence(bytes, b"\x1b[5n") {
        responses.push("\x1b[0n".to_string());
    }
    if contains_sequence(bytes, b"\x1b[6n") || contains_sequence(bytes, b"\x1b[?6n") {
        let (row, col) = cursor_position;
        responses.push(format!("\x1b[{};{}R", row.saturating_add(1), col.saturating_add(1)));
    }
    if contains_sequence(bytes, b"\x1b[c") || contains_sequence(bytes, b"\x1b[0c") {
        responses.push("\x1b[?1;2c".to_string());
    }
    if contains_sequence(bytes, b"\x1b[>c") || contains_sequence(bytes, b"\x1b[>0c") {
        responses.push("\x1b[>0;276;0c".to_string());
    }
    responses
}

fn contains_sequence(haystack: &[u8], needle: &[u8]) -> bool {
    haystack.windows(needle.len()).any(|candidate| candidate == needle)
}

fn emit_state(stdout: &Arc<Mutex<io::Stdout>>, terminal_id: &str, session: &Session, full_frame: bool) {
    emit_state_parts(
        stdout,
        terminal_id,
        session.cols,
        session.rows,
        &session.state,
        &session.dirty_rows,
        full_frame,
    );
}

fn emit_state_parts(
    stdout: &Arc<Mutex<io::Stdout>>,
    terminal_id: &str,
    cols: u16,
    rows: u16,
    state: &Arc<Mutex<TerminalState>>,
    dirty_rows: &Arc<Mutex<HashSet<usize>>>,
    full_frame: bool,
) {
    let state = match state.lock() {
        Ok(state) => state,
        Err(_) => return,
    };
    let screen = state.parser.screen();
    let row_indexes = if full_frame {
        (0..rows as usize).collect::<Vec<_>>()
    } else {
        let mut dirty = match dirty_rows.lock() {
            Ok(dirty) => dirty,
            Err(_) => return,
        };
        let mut rows = dirty.iter().copied().collect::<Vec<_>>();
        rows.sort_unstable();
        dirty.clear();
        rows
    };
    let mut cells = serde_json::Map::new();
    for row in row_indexes {
        let mut row_cells = Vec::with_capacity(cols as usize);
        for col in 0..cols {
            row_cells.push(cell_to_json(screen.cell(row as u16, col)));
        }
        cells.insert(row.to_string(), serde_json::to_value(row_cells).unwrap());
    }
    let (cursor_y, cursor_x) = screen.cursor_position();
    emit(stdout, json!({
        "event": "state",
        "terminalId": terminal_id,
        "fullFrame": full_frame,
        "cols": cols,
        "rows": rows,
        "cells": cells,
        "cursorX": cursor_x.min(cols.saturating_sub(1)),
        "cursorY": cursor_y.min(rows.saturating_sub(1)),
        "cursorVisible": !screen.hide_cursor(),
        "cursorStyle": "block",
        "appCursorKeys": screen.application_cursor(),
        "bracketedPaste": screen.bracketed_paste(),
        "mouseMode": mode_or_null(format!("{:?}", screen.mouse_protocol_mode())),
        "mouseEncoding": mode_or_null(format!("{:?}", screen.mouse_protocol_encoding())),
        "reverseVideo": false,
        "scrollbackLength": state.version
    }));
}

fn cell_to_json(cell: Option<&vt100::Cell>) -> CellJson {
    let Some(cell) = cell else {
        return CellJson { ch: " ".to_string(), fg: None, bg: None, attrs: vec![] };
    };
    let mut fg = color_to_hex(cell.fgcolor());
    let mut bg = color_to_hex(cell.bgcolor());
    let mut attrs = Vec::new();
    if cell.bold() {
        attrs.push("bold".to_string());
    }
    if cell.dim() {
        attrs.push("dim".to_string());
    }
    if cell.italic() {
        attrs.push("italic".to_string());
    }
    if cell.underline() {
        attrs.push("underline".to_string());
    }
    if cell.inverse() {
        std::mem::swap(&mut fg, &mut bg);
        attrs.push("inverse".to_string());
    }
    CellJson {
        ch: if cell.is_wide_continuation() {
            " ".to_string()
        } else if cell.has_contents() {
            cell.contents().to_string()
        } else {
            " ".to_string()
        },
        fg,
        bg,
        attrs,
    }
}

fn color_to_hex(color: vt100::Color) -> Option<String> {
    match color {
        vt100::Color::Default => None,
        vt100::Color::Rgb(red, green, blue) => Some(format!("#{red:02X}{green:02X}{blue:02X}")),
        vt100::Color::Idx(index) => Some(indexed_color_to_hex(index)),
    }
}

fn indexed_color_to_hex(index: u8) -> String {
    const ANSI_16: [&str; 16] = [
        "#000000", "#CD0000", "#00CD00", "#CDCD00", "#0000EE", "#CD00CD", "#00CDCD", "#E5E5E5",
        "#7F7F7F", "#FF0000", "#00FF00", "#FFFF00", "#5C5CFF", "#FF00FF", "#00FFFF", "#FFFFFF",
    ];
    if index < 16 {
        return ANSI_16[index as usize].to_string();
    }
    if (16..=231).contains(&index) {
        let value = index - 16;
        let red = color_cube_component(value / 36);
        let green = color_cube_component((value % 36) / 6);
        let blue = color_cube_component(value % 6);
        return format!("#{red:02X}{green:02X}{blue:02X}");
    }
    let level = 8 + (index.saturating_sub(232) * 10);
    format!("#{level:02X}{level:02X}{level:02X}")
}

fn color_cube_component(value: u8) -> u8 {
    if value == 0 { 0 } else { 55 + value * 40 }
}

fn mode_or_null(value: String) -> Value {
    if value == "None" {
        Value::Null
    } else {
        json!(value)
    }
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn normalize_terminal_input_converts_line_feed_to_carriage_return() {
        assert_eq!(normalize_terminal_input("echo test\n"), "echo test\r\n");
        assert_eq!(normalize_terminal_input("echo test\r\n"), "echo test\r\n");
        assert_eq!(normalize_terminal_input("echo test\r"), "echo test\r\n");
    }

    #[test]
    fn vt100_parser_preserves_crlf_output() {
        let mut parser = vt100::Parser::new(4, 40, DEFAULT_SCROLLBACK);
        parser.process(b"first\r\nsecond\r\n");

        let text = parser.screen().contents();

        assert!(text.contains("first"));
        assert!(text.contains("second"));
    }

    #[test]
    fn terminal_query_responses_answer_cursor_position_report() {
        let responses = terminal_query_responses(b"\x1b[6n", (2, 4));

        assert_eq!(responses, vec!["\x1b[3;5R".to_string()]);
    }

    #[test]
    fn terminal_query_responses_answer_device_status_and_attributes() {
        let responses = terminal_query_responses(b"\x1b[5n\x1b[c\x1b[>c", (0, 0));

        assert!(responses.contains(&"\x1b[0n".to_string()));
        assert!(responses.contains(&"\x1b[?1;2c".to_string()));
        assert!(responses.contains(&"\x1b[>0;276;0c".to_string()));
    }
}
