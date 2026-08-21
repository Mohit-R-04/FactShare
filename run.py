#!/usr/bin/env python3
"""
FactShare - one-command local launcher.

Starts the full app stack and manages its lifecycle:

    database         embedded H2 (dev) or PostgreSQL (JDBC_DATABASE_URL)
    ai-service       Flask + Google Gemini (Python)                 port 5002
    minimax-service  Flask + NVIDIA NIM chat (optional)             port 5003
    backend-spring   Spring Boot (Java 21, Maven)                   port 5001
    frontend         React dev server                               port 3000

Usage:
    python3 run.py                  # start everything
    python3 run.py --open           # also open http://localhost:3000 in a browser
    python3 run.py --no-minimax     # skip the optional NVIDIA chat service
    python3 run.py --skip-setup     # do not create venv / install dependencies
    python3 run.py --watch          # tail all service logs to the console

The backend uses an embedded H2 database by default (no DB process to run).
Set JDBC_DATABASE_URL / DATABASE_USER / DATABASE_PASSWORD in .env to use
PostgreSQL instead.

All environment variables are read from the root .env file. Logs go to .run-logs/.
Press Ctrl+C to stop every service cleanly.
"""

import argparse
import os
import shutil
import signal
import socket
import subprocess
import sys
import time
import webbrowser
from pathlib import Path

ROOT = Path(__file__).resolve().parent
LOGS_DIR = ROOT / ".run-logs"
ENV_FILE = ROOT / ".env"

AI_DIR = ROOT / "ai-service"
BACKEND_DIR = ROOT / "backend-spring"
FRONTEND_DIR = ROOT / "frontend"

GEMINI_PORT = 5002
MINIMAX_PORT = 5003
BACKEND_PORT = 5001
FRONTEND_PORT = 3000


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------

def log(msg):
    print(f"[run] {msg}")


def warn(msg):
    print(f"[warn] {msg}", file=sys.stderr)


def load_dotenv(path):
    """Parse a simple KEY=VALUE .env file. No third-party dependency."""
    env = {}
    if not path.exists():
        return env
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key:
            env[key] = value
    return env


def cmd_for(name):
    """Resolve a CLI tool, falling back to its Windows .cmd variant."""
    found = shutil.which(name)
    if found:
        return found
    return f"{name}.cmd" if os.name == "nt" else name


def venv_python(venv_dir):
    return venv_dir / "Scripts" / "python.exe" if os.name == "nt" else venv_dir / "bin" / "python"


def venv_pip(venv_dir):
    return venv_dir / "Scripts" / "pip.exe" if os.name == "nt" else venv_dir / "bin" / "pip"


def port_open(port, host="127.0.0.1", timeout=1.0):
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except OSError:
        return False


def wait_ready(port, timeout, what):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if port_open(port):
            return True
        time.sleep(0.75)
    return False


def tail(path, n=60):
    try:
        lines = path.read_text().splitlines()
    except OSError:
        return
    print(f"--- last {n} lines of {path} ---")
    for line in lines[-n:]:
        print(f"    {line}")


def spawn(cmd, cwd, env, logfile):
    """Start a long-running child in its own process group, logging to logfile."""
    logfile.parent.mkdir(parents=True, exist_ok=True)
    fh = open(logfile, "ab")
    proc = subprocess.Popen(
        cmd,
        cwd=str(cwd),
        env=env,
        stdout=fh,
        stderr=subprocess.STDOUT,
        start_new_session=True,
    )
    fh.close()
    return proc


def stop(proc):
    if proc is None or proc.poll() is not None:
        return
    try:
        if os.name == "posix":
            os.killpg(os.getpgid(proc.pid), signal.SIGTERM)
        else:
            proc.terminate()
    except (ProcessLookupError, PermissionError):
        return
    try:
        proc.wait(timeout=10)
    except subprocess.TimeoutExpired:
        try:
            if os.name == "posix":
                os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
            else:
                proc.kill()
        except Exception:
            pass


def run_step(label, cmd, cwd, env):
    """Run a one-shot setup command with live console output."""
    log(f"setup: {label}")
    try:
        result = subprocess.run(cmd, cwd=str(cwd), env=env)
    except OSError as e:
        warn(f"failed to run {cmd[0]}: {e}")
        return False
    if result.returncode != 0:
        warn(f"setup step failed: {label}")
        return False
    return True


class LogWatcher:
    """Incrementally prints new lines appended to the service log files."""

    def __init__(self, paths):
        self.offsets = {p: (p.stat().st_size if p.exists() else 0) for p in paths}

    def poll(self):
        for path in list(self.offsets):
            try:
                size = path.stat().st_size
            except OSError:
                continue
            offset = self.offsets[path]
            if size <= offset:
                continue
            with open(path, "rb") as f:
                f.seek(offset)
                data = f.read()
            self.offsets[path] = size
            for line in data.decode("utf-8", "replace").splitlines():
                print(f"[{path.name}] {line}")


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Start the full FactShare stack (backend + AI services + frontend)."
    )
    parser.add_argument("--open", action="store_true", help="open http://localhost:3000 when ready")
    parser.add_argument("--no-minimax", action="store_true", help="skip the optional NVIDIA chat service")
    parser.add_argument("--skip-setup", action="store_true", help="do not create venv / install dependencies")
    parser.add_argument("--watch", action="store_true", help="tail all service logs to the console")
    args = parser.parse_args()

    # 1. banner + env
    print("=" * 62)
    print("  FactShare - local development stack")
    print("=" * 62)

    dotenv = load_dotenv(ENV_FILE)
    if not dotenv:
        warn(f"{ENV_FILE} not found or empty - services may lack required config")
    env = dict(os.environ)
    for key, value in dotenv.items():
        env.setdefault(key, value)

    # 2. preflight checks
    required = {"java": "Java 21", "mvn": "Maven 3.9+", "node": "Node.js 18+", "npm": "npm", "python3": "Python 3.10+"}
    missing = [label for name, label in required.items() if not shutil.which(name)]
    if missing:
        warn("missing required tools: " + ", ".join(missing))
        sys.exit(1)

    # 3. one-time setup (venv, deps, backend build)
    ai_py = venv_python(AI_DIR / ".venv")
    if args.skip_setup:
        if not ai_py.exists():
            warn(f"{ai_py} does not exist - rerun without --skip-setup to create it")
            sys.exit(1)
    else:
        if not ai_py.exists():
            if not run_step("create ai-service virtualenv",
                            [sys.executable, "-m", "venv", str(AI_DIR / ".venv")], AI_DIR, env):
                sys.exit(1)
        if not ai_py.exists():
            warn("failed to create ai-service venv")
            sys.exit(1)
        if not venv_pip(AI_DIR / ".venv").exists():
            if not run_step("install ai-service python dependencies",
                            [str(ai_py), "-m", "pip", "install", "-r", "requirements.txt"], AI_DIR, env):
                sys.exit(1)
        if not (FRONTEND_DIR / "node_modules").exists():
            if not run_step("install frontend npm dependencies",
                            [cmd_for("npm"), "install"], FRONTEND_DIR, env):
                sys.exit(1)
        if not list((BACKEND_DIR / "target").glob("*.jar")):
            log("backend not built yet - running mvn package (first build downloads deps)")
            if not run_step("build backend (mvn package)",
                            [cmd_for("mvn"), "-q", "-DskipTests", "package"], BACKEND_DIR, env):
                sys.exit(1)

    # 4. service definitions
    services = []

    if port_open(GEMINI_PORT):
        log(f"ai-service already running on port {GEMINI_PORT} - reusing it")
    else:
        services.append({
            "name": "ai-service",
            "cmd": [str(ai_py), "gemini_service.py"],
            "cwd": AI_DIR,
            "port": GEMINI_PORT,
            "timeout": 45,
            "log": LOGS_DIR / "ai-service.log",
            "url": f"http://localhost:{GEMINI_PORT}",
        })

    if not args.no_minimax and env.get("NVIDIA_API_KEY"):
        if port_open(MINIMAX_PORT):
            log(f"minimax-service already running on port {MINIMAX_PORT} - reusing it")
        else:
            services.append({
                "name": "minimax-service",
                "cmd": [str(ai_py), "minimax_service.py"],
                "cwd": AI_DIR,
                "port": MINIMAX_PORT,
                "timeout": 45,
                "log": LOGS_DIR / "minimax-service.log",
                "url": f"http://localhost:{MINIMAX_PORT}",
            })
    else:
        log("minimax-service skipped (pass --no-minimax to silence, or set NVIDIA_API_KEY to enable)")

    if port_open(BACKEND_PORT):
        log(f"backend already running on port {BACKEND_PORT} - reusing it")
    else:
        services.append({
            "name": "backend-spring",
            "cmd": [cmd_for("mvn"), "spring-boot:run"],
            "cwd": BACKEND_DIR,
            "port": BACKEND_PORT,
            "timeout": 360,
            "log": LOGS_DIR / "backend.log",
            "url": f"http://localhost:{BACKEND_PORT}",
        })

    if port_open(FRONTEND_PORT):
        log(f"frontend already running on port {FRONTEND_PORT} - reusing it")
    else:
        services.append({
            "name": "frontend",
            "cmd": [cmd_for("npm"), "start"],
            "cwd": FRONTEND_DIR,
            "port": FRONTEND_PORT,
            "timeout": 240,
            "log": LOGS_DIR / "frontend.log",
            "url": f"http://localhost:{FRONTEND_PORT}",
            # Pin the dev-server port: the root .env sets PORT=5001 for Spring
            # Boot, which react-scripts would otherwise inherit and collide on.
            "extra_env": {"BROWSER": "none", "PORT": str(FRONTEND_PORT)},
        })

    if not services:
        log("nothing to start - all services are already running")
        return

    # 6. spawn everything, then wait for each to become ready
    for s in services:
        child_env = dict(env)
        child_env.update(s.get("extra_env", {}))
        log(f"starting {s['name']} ...")
        s["proc"] = spawn(s["cmd"], s["cwd"], child_env, s["log"])

    failed = None
    for s in services:
        if wait_ready(s["port"], s["timeout"], s["name"]):
            log(f"{s['name']} is up at {s['url']}")
        else:
            failed = s
            break

    if failed:
        tail(failed["log"])
        warn(f"{failed['name']} did not become ready within {failed['timeout']}s - see {failed['log']}")
        for s in services:
            stop(s["proc"])
        sys.exit(1)

    # 7. summary
    print()
    print("=" * 62)
    print("  All services are up:")
    print("=" * 62)
    rows = [("Service", "URL / Status")]
    rows.append(("frontend", f"http://localhost:{FRONTEND_PORT}"))
    rows.append(("backend-spring", f"http://localhost:{BACKEND_PORT}"))
    rows.append(("ai-service (Gemini)", f"http://localhost:{GEMINI_PORT}"))
    if not args.no_minimax and env.get("NVIDIA_API_KEY"):
        rows.append(("minimax-service (NVIDIA)", f"http://localhost:{MINIMAX_PORT}"))
    rows.append(("database", "H2 (embedded) / PostgreSQL via JDBC_DATABASE_URL"))
    width = max(len(r[0]) for r in rows) + 2
    for label, value in rows:
        print(f"  {label:<{width}}{value}")
    print()
    print(f"  Logs:   {LOGS_DIR}/<service>.log")
    print("  Press Ctrl+C to stop all services.")
    print("=" * 62)

    if args.open:
        webbrowser.open(f"http://localhost:{FRONTEND_PORT}")

    # 8. monitor children; handle SIGINT/SIGTERM
    def _term_handler(signum, frame):
        raise KeyboardInterrupt

    signal.signal(signal.SIGTERM, _term_handler)

    watcher = LogWatcher([s["log"] for s in services]) if args.watch else None

    try:
        while True:
            for s in services:
                if s["proc"].poll() is not None:
                    raise RuntimeError(f"{s['name']} exited unexpectedly (code {s['proc'].returncode})")
            if watcher:
                watcher.poll()
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nStopping all services...")
    except RuntimeError as e:
        warn(str(e))
        for s in services:
            if s["proc"].poll() is not None:
                tail(s["log"])
    finally:
        for s in services:
            stop(s["proc"])
        log("done")


if __name__ == "__main__":
    main()
