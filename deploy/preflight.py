import argparse
import datetime
import json
import pathlib
import shlex
import shutil
import subprocess
from dataclasses import dataclass


@dataclass
class Snapshot:
    os_id: str
    os_version: str
    available_mib: int
    free_disk_gib: float
    listening_ports: list
    ports_known: bool
    esm_enabled: bool
    docker_present: bool
    compose_present: bool


def assess(snapshot, today=None):
    today = today or datetime.date.today()
    blockers = []
    standard_support = {
        "22.04": datetime.date(2027, 5, 31),
        "24.04": datetime.date(2029, 5, 31),
    }
    supported = snapshot.os_id == "ubuntu" and (
        today <= standard_support.get(snapshot.os_version, datetime.date.min)
        or (snapshot.os_version == "20.04" and snapshot.esm_enabled)
    )
    if not supported:
        blockers.append("Ubuntu security coverage is absent or not recognized. Do not deploy until the operator verifies coverage and applies required fixes.")
    if snapshot.available_mib < 600:
        blockers.append("Less than 600 MiB RAM is currently available for the 400 MiB profile plus a 200 MiB coexistence margin.")
    if snapshot.free_disk_gib < 3:
        blockers.append("Less than 3 GiB disk is free; images, database growth and backups need reviewed capacity.")
    if not snapshot.ports_known:
        blockers.append("Could not inspect listening TCP ports. Review them before binding ports 80/443.")
    elif set(snapshot.listening_ports).intersection({80, 443}):
        blockers.append("TCP 80 or 443 is already occupied. Preserve the existing service and review proxy integration.")
    if not snapshot.docker_present or not snapshot.compose_present:
        blockers.append("Approved Docker/Compose tooling is not present. No software was installed; an operator must choose the deployment approach.")
    return blockers


def run_readonly(arguments):
    try:
        result = subprocess.run(arguments, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
                                text=True, timeout=15, check=False)
        return result.stdout if result.returncode == 0 else None
    except (OSError, subprocess.TimeoutExpired):
        return None


def collect():
    release = {}
    try:
        for line in pathlib.Path("/etc/os-release").read_text().splitlines():
            if "=" in line:
                name, value = line.split("=", 1)
                parts = shlex.split(value)
                release[name] = parts[0] if parts else ""
    except (OSError, ValueError):
        pass
    available = 0
    try:
        for line in pathlib.Path("/proc/meminfo").read_text().splitlines():
            if line.startswith("MemAvailable:"):
                available = int(line.split()[1]) // 1024
    except (OSError, ValueError, IndexError):
        pass
    esm = False
    status = run_readonly(["pro", "status", "--format", "json"])
    if status:
        try:
            document = json.loads(status)
            enabled = {service.get("name") for service in document.get("services", [])
                       if service.get("status") == "enabled"}
            esm = document.get("attached") is True and {"esm-infra", "esm-apps"}.issubset(enabled)
        except (ValueError, TypeError, AttributeError):
            pass
    ports = []
    listeners = run_readonly(["ss", "-H", "-ltn"])
    if listeners is not None:
        for line in listeners.splitlines():
            columns = line.split()
            if len(columns) >= 4:
                port = columns[3].rsplit(":", 1)[-1]
                if port.isdigit():
                    ports.append(int(port))
    try:
        disk = shutil.disk_usage("/").free / (1024 ** 3)
    except OSError:
        disk = 0
    return Snapshot(release.get("ID", "unknown"), release.get("VERSION_ID", "unknown"),
                    available, round(disk, 2), sorted(set(ports)), listeners is not None,
                    esm, shutil.which("docker") is not None,
                    run_readonly(["docker", "compose", "version", "--short"]) is not None)


def main():
    parser = argparse.ArgumentParser(description="Read-only VM deployment preflight. Run inside the target Ubuntu VM, not Cloud Shell.")
    parser.add_argument("--json", action="store_true", help="Print only nonsensitive readiness facts and blockers as JSON")
    arguments = parser.parse_args()
    snapshot = collect()
    blockers = assess(snapshot)
    report = {
        "os": "{} {}".format(snapshot.os_id, snapshot.os_version),
        "available_memory_mib": snapshot.available_mib,
        "free_disk_gib": snapshot.free_disk_gib,
        "esm_enabled": snapshot.esm_enabled,
        "https_ports_available": snapshot.ports_known and not set(snapshot.listening_ports).intersection({80, 443}),
        "docker_present": snapshot.docker_present,
        "compose_present": snapshot.compose_present,
        "eligible_for_staging_review": not blockers,
        "blockers": blockers,
        "notice": "No changes made. A pass is not approval to deploy: patch status, peak Telethon usage, backups, DNS/TLS and rollback still require review.",
    }
    if arguments.json:
        print(json.dumps(report, indent=2))
    else:
        for name, value in report.items():
            print("{}: {}".format(name, value))
    return 2 if blockers else 0


if __name__ == "__main__":
    raise SystemExit(main())