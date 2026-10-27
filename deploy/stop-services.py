"""Stop this checkout's Windows services without removing containers or volumes."""

import ctypes
import json
import os
import shutil
import socket
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def powershell(script):
    result = subprocess.run(
        ["powershell.exe", "-NoProfile", "-Command", script],
        capture_output=True,
        encoding="utf-8",
        errors="replace",
        check=True,
    )
    return result.stdout.strip()


def arguments(command):
    """Use Windows parsing so quoted paths cannot match another checkout by prefix."""
    if not command:
        return []
    shell = ctypes.windll.shell32
    shell.CommandLineToArgvW.argtypes = [ctypes.c_wchar_p, ctypes.POINTER(ctypes.c_int)]
    shell.CommandLineToArgvW.restype = ctypes.POINTER(ctypes.c_wchar_p)
    count = ctypes.c_int()
    pointer = shell.CommandLineToArgvW(command, ctypes.byref(count))
    if not pointer:
        raise RuntimeError("Could not parse process command line")
    try:
        return [pointer[index] for index in range(count.value)]
    finally:
        ctypes.windll.kernel32.LocalFree.argtypes = [ctypes.c_void_p]
        ctypes.windll.kernel32.LocalFree(pointer)


def same_path(value, expected):
    return os.path.normcase(os.path.abspath(value)) == os.path.normcase(
        str(expected.resolve())
    )


def belongs_to_project(service, process):
    args = arguments(process.get("CommandLine"))
    name = process.get("Name", "").lower()
    if service == "ai-service":
        executable = ROOT / "apps/ai-service/.venv/Scripts/python.exe"
        return (
            name in ("python.exe", "pythonw.exe")
            and bool(args)
            and same_path(args[0], executable)
            and "app.main:create_app" in args
            and (
                "uvicorn" in args
                or any(
                    same_path(arg, executable.with_name("uvicorn.exe")) for arg in args
                )
            )
        )
    if service == "backend":
        parent_args = arguments(process.get("ParentCommandLine"))
        prefix = "-Dmaven.multiModuleProjectDirectory="
        return (
            name == "java.exe"
            and "com.normal.picturebackend.NormalPictureBackendApplication" in args
            and "spring-boot:run" in parent_args
            and any(
                arg.startswith(prefix)
                and same_path(arg[len(prefix) :], ROOT / "apps/backend")
                for arg in parent_args
            )
        )
    return False


def listeners(port):
    # Only numeric ports are interpolated. Command lines are captured, never printed.
    output = powershell(
        "[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new(); "
        "$ErrorActionPreference = 'Stop'; "
        f"$owners = @(Get-NetTCPConnection -State Listen -LocalPort {int(port)} "
        "-ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique); "
        "$items = @(foreach ($ownerId in $owners) { "
        '$p = Get-CimInstance Win32_Process -Filter "ProcessId = $ownerId"; '
        "if ($null -eq $p) { continue }; "
        '$parent = Get-CimInstance Win32_Process -Filter "ProcessId = $($p.ParentProcessId)"; '
        "$native = Get-Process -Id $ownerId; "
        "[pscustomobject]@{Id=$ownerId; Name=$p.Name; CommandLine=$p.CommandLine; "
        "ParentCommandLine=$parent.CommandLine; "
        "StartTicks=$native.StartTime.ToFileTimeUtc().ToString()} }); "
        "ConvertTo-Json -InputObject $items -Compress"
    )
    return json.loads(output)


def port_open(port):
    with socket.socket() as connection:
        connection.settimeout(1)
        return connection.connect_ex(("127.0.0.1", port)) == 0


def stop_process(service, process):
    process_id = int(process["Id"])
    ticks = int(process["StartTicks"])
    # Revalidate creation time immediately before stopping to reject recycled PIDs.
    powershell(
        "$ErrorActionPreference = 'Stop'; "
        f"$p = Get-Process -Id {process_id} -ErrorAction SilentlyContinue; "
        "if ($null -eq $p) { exit 0 }; "
        f"if ($p.StartTime.ToFileTimeUtc() -ne [long]{ticks}) "
        "{ throw 'PID identity changed; refusing to stop' }; "
        "Stop-Process -InputObject $p -ErrorAction Stop; "
        "if (-not $p.WaitForExit(15000)) { throw 'Process stop timed out' }"
    )
    print(f"[OK] {service} stopped (PID {process_id}).", flush=True)


def compose_command():
    command = ["docker", "compose"]
    if (ROOT / "deploy/infra.env").exists():
        command += ["--env-file", "deploy/infra.env"]
    return command + ["-f", "deploy/docker-compose.infra.yml"]


def main():
    if os.name != "nt":
        raise RuntimeError("This launcher requires Windows")
    for executable in ("powershell.exe", "docker"):
        if not shutil.which(executable):
            raise RuntimeError(f"Required command is missing from PATH: {executable}")
    compose = compose_command()
    subprocess.run(compose + ["config", "--quiet"], cwd=ROOT, check=True)
    subprocess.run(["docker", "info"], capture_output=True, check=True)
    targets = []
    for service, port in (("backend", 8333), ("ai-service", 8334)):
        processes = listeners(port)
        if not processes and port_open(port):
            raise RuntimeError(
                f"Cannot identify the owner of port {port}; nothing was stopped"
            )
        for process in processes:
            if not belongs_to_project(service, process):
                raise RuntimeError(
                    f"Port {port} belongs to an unverified process (PID {process['Id']}); "
                    "nothing was stopped. Only this checkout's launcher processes are supported."
                )
        targets.append((service, port, processes))
    # Verify both applications before stopping either or their dependencies.
    for service, port, processes in targets:
        if not processes:
            print(f"[OK] {service} already stopped.", flush=True)
        for process in processes:
            stop_process(service, process)
        if port_open(port):
            raise RuntimeError(
                f"Port {port} is still occupied; infrastructure was not stopped"
            )
    print(
        "[STOP] MySQL / Redis / MinIO; preserving containers and volumes.", flush=True
    )
    subprocess.run(
        compose + ["stop", "--timeout", "30", "mysql", "redis", "minio"],
        cwd=ROOT,
        check=True,
    )
    result = subprocess.run(
        compose + ["ps", "--status", "running", "--services"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=True,
    )
    if {"mysql", "redis", "minio"}.intersection(result.stdout.splitlines()):
        raise RuntimeError("Some infrastructure containers are still running")
    print(
        "All services stopped. Containers, networks and persistent volumes are retained."
    )


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, ValueError, subprocess.CalledProcessError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        print(
            "Stopping was incomplete; inspect service status before retrying.",
            file=sys.stderr,
        )
        sys.exit(1)
