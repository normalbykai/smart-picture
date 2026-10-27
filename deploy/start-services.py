"""Windows local service launcher. Run via start-services.cmd or uv run."""

import json
import os
import secrets
import shutil
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parents[1]
AI_ROOT = ROOT / "apps/ai-service"
BACKEND_ROOT = ROOT / "apps/backend"
LOG_ROOT = ROOT / "logs"
HTTP = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def compose_command():
    command = ["docker", "compose"]
    if (ROOT / "deploy/infra.env").exists():
        command += ["--env-file", "deploy/infra.env"]
    return command + ["-f", "deploy/docker-compose.infra.yml"]


def host_port(service, target):
    for port in service["ports"]:
        if int(port["target"]) == target:
            if port.get("host_ip", "0.0.0.0") not in ("127.0.0.1", "0.0.0.0"):
                raise RuntimeError(
                    "Service ports must be reachable through IPv4 localhost"
                )
            return int(port["published"])
    raise RuntimeError(f"Missing host mapping for container port {target}")


def connection_environment(config, api_key):
    services = config["services"]
    mysql = services["mysql"]["environment"]
    minio = services["minio"]["environment"]
    return {
        "DB_HOST": "127.0.0.1",
        "DB_PORT": str(host_port(services["mysql"], 3306)),
        "DB_NAME": mysql["MYSQL_DATABASE"],
        "DB_USERNAME": mysql["MYSQL_USER"],
        "DB_PASSWORD": mysql["MYSQL_PASSWORD"],
        "REDIS_HOST": "127.0.0.1",
        "REDIS_PORT": str(host_port(services["redis"], 6379)),
        "REDIS_PASSWORD": "",
        "MINIO_ENDPOINT": f"http://127.0.0.1:{host_port(services['minio'], 9000)}",
        "MINIO_ACCESS_KEY": minio["MINIO_ROOT_USER"],
        "MINIO_SECRET_KEY": minio["MINIO_ROOT_PASSWORD"],
        "AI_SERVICE_API_KEY": api_key,
        "AI_SERVICE_BASE_URL": "http://127.0.0.1:8334",
        "SERVER_PORT": "8333",
        "SERVER_SERVLET_CONTEXT_PATH": "/api",
    }


def prepare_configuration():
    env_file = AI_ROOT / ".env"
    if not env_file.exists():
        # Exclusive creation prevents overwriting a configuration created concurrently.
        with env_file.open("x", encoding="utf-8") as stream:
            stream.write(f"AI_SERVICE_API_KEY={secrets.token_urlsafe(32)}\n")
            stream.write("AI_SERVICE_DOCS_ENABLED=true\n")
    api_key = os.environ.get("AI_SERVICE_API_KEY") or dotenv_values(env_file).get(
        "AI_SERVICE_API_KEY"
    )
    if (
        not api_key
        or len(api_key) < 32
        or any(not 33 <= ord(c) <= 126 for c in api_key)
    ):
        raise RuntimeError("Set a valid AI_SERVICE_API_KEY in apps/ai-service/.env")
    local = BACKEND_ROOT / "src/main/resources/application-local.yml"
    if not local.exists():
        with local.open("x", encoding="utf-8") as stream:
            stream.write(
                local.with_name("application-local.example.yml").read_text("utf-8")
            )
    return api_key


def request_json(url, headers=None):
    request = urllib.request.Request(url, headers=headers or {})
    try:
        with HTTP.open(request, timeout=3) as response:
            return json.load(response)
    except (OSError, ValueError, urllib.error.URLError):
        return None


def ai_ready(api_key):
    body = request_json("http://127.0.0.1:8334/api/v1/info", {"X-API-Key": api_key})
    return (
        isinstance(body, dict)
        and body.get("code") == 0
        and isinstance(body.get("data"), dict)
        and body["data"].get("name") == "smart-picture-ai-service"
    )


def backend_ready():
    # Anonymous access must reach the application's existing authentication guard.
    body = request_json("http://127.0.0.1:8333/api/ai/info")
    return isinstance(body, dict) and body.get("code") == 40100


def port_in_use(port):
    with socket.socket() as connection:
        connection.settimeout(1)
        return connection.connect_ex(("127.0.0.1", port)) == 0


def ensure_service(name, port, ready, command, cwd, environment, timeout=240):
    if ready():
        print(
            f"[OK] {name} already running on {port}; keeping current process.",
            flush=True,
        )
        return
    if port_in_use(port):
        raise RuntimeError(
            f"Port {port} is occupied but {name} verification failed. "
            "Check the existing process/configuration; no process was terminated."
        )
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    stdout_path = LOG_ROOT / f"{name}-{stamp}.stdout.log"
    stderr_path = LOG_ROOT / f"{name}-{stamp}.stderr.log"
    print(f"[START] {name}; logs: {stdout_path}", flush=True)
    # Launch in a separate native command shell; do not tie servers to this launcher.
    # Commands below are fixed tokens, and cwd/env are passed separately from shell text.
    with stdout_path.open("wb") as stdout, stderr_path.open("wb") as stderr:
        process = subprocess.Popen(
            ["cmd.exe", "/d", "/c", *command],
            cwd=cwd,
            env=environment,
            stdin=subprocess.DEVNULL,
            stdout=stdout,
            stderr=stderr,
            creationflags=subprocess.CREATE_NO_WINDOW,
        )
    deadline = time.monotonic() + timeout
    next_update = time.monotonic() + 15
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError(
                f"{name} exited; inspect {stdout_path} and {stderr_path}"
            )
        if ready():
            print(f"[OK] {name} ready on {port} (shell PID {process.pid}).", flush=True)
            return
        if time.monotonic() >= next_update:
            print(f"[WAIT] {name} is starting; see {stdout_path}", flush=True)
            next_update = time.monotonic() + 15
        time.sleep(1)
    raise RuntimeError(
        f"{name} readiness timed out; inspect {stdout_path} and {stderr_path}"
    )


def main():
    if os.name != "nt":
        raise RuntimeError(
            "This launcher requires Windows; use the documented commands elsewhere"
        )
    for executable in ("docker", "uv", "mvn", "java"):
        if shutil.which(executable) is None:
            raise RuntimeError(f"Required command is missing from PATH: {executable}")
    LOG_ROOT.mkdir(exist_ok=True)
    api_key = prepare_configuration()
    compose = compose_command()
    # Capture rendered configuration privately: it includes database/storage credentials.
    rendered = subprocess.run(
        compose + ["config", "--format", "json"],
        cwd=ROOT,
        capture_output=True,
        check=True,
    )
    config = json.loads(rendered.stdout)
    environment = os.environ.copy()
    environment.update(connection_environment(config, api_key))
    print("[START] MySQL / Redis / MinIO (waiting for healthy status)", flush=True)
    subprocess.run(
        compose + ["up", "-d", "--wait", "--wait-timeout", "180"], cwd=ROOT, check=True
    )
    ensure_service(
        "ai-service",
        8334,
        lambda: ai_ready(api_key),
        [
            "uv",
            "run",
            "--locked",
            "uvicorn",
            "app.main:create_app",
            "--factory",
            "--host",
            "127.0.0.1",
            "--port",
            "8334",
            "--no-access-log",
        ],
        AI_ROOT,
        environment,
    )
    ensure_service(
        "backend",
        8333,
        backend_ready,
        ["mvn", "-B", "spring-boot:run", "-Dspring-boot.run.profiles=local"],
        BACKEND_ROOT,
        environment,
    )
    print("\nAll services are ready.", flush=True)
    print("Spring Boot: http://127.0.0.1:8333/api")
    print("Python AI:   http://127.0.0.1:8334/docs")
    print(
        f"MinIO:       http://127.0.0.1:{host_port(config['services']['minio'], 9001)}"
    )
    print(
        "GET /api/ai/info requires an administrator login; startup does not create test users."
    )


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, subprocess.CalledProcessError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        print(
            "Already started services remain running. Fix the issue and run again.",
            file=sys.stderr,
        )
        sys.exit(1)
