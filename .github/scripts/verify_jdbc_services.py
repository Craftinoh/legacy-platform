#!/usr/bin/env python3
"""Verifica i provider JDBC nei JAR Velocity prodotti da Shadow."""

from __future__ import annotations

import os
import subprocess
import tempfile
from pathlib import Path
from zipfile import ZipFile

ARTIFACTS = {
    "NetworkLanguage": Path("language-velocity/build/libs"),
    "LegacyChickenWarsProxy": Path("chickenwars-velocity/build/libs"),
    "LegacyReports": Path("legacy-reports-velocity/build/libs"),
    "LegacyScreenshare": Path("legacy-screenshare-velocity/build/libs"),
}

PROBE_SOURCE = """\
import java.sql.Driver;
import java.sql.DriverManager;

public final class JdbcDriverProbe {
    public static void main(String[] args) throws Exception {
        Driver driver = DriverManager.getDriver(args[0]);
        System.out.println(driver.getClass().getName());
    }
}
"""


def find_jar(prefix: str, directory: Path) -> Path:
    matches = sorted(
        path for path in directory.glob(f"{prefix}-*.jar")
        if not path.name.endswith("-api.jar")
        and not path.name.endswith("-internal.jar")
    )
    if len(matches) != 1:
        raise SystemExit(
            f"{prefix}: atteso un solo JAR in {directory}, trovati {matches}"
        )
    return matches[0]


def providers_from(jar: ZipFile) -> list[str]:
    name = "META-INF/services/java.sql.Driver"
    try:
        raw = jar.read(name).decode("utf-8")
    except KeyError as exc:
        raise SystemExit(f"{jar.filename}: manca {name}") from exc
    return [
        line.strip()
        for line in raw.splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]


def run_probe(jar_path: Path, probe_dir: Path, jdbc_url: str) -> str:
    classpath = os.pathsep.join((str(jar_path.resolve()), str(probe_dir)))
    result = subprocess.run(
        ["java", "-cp", classpath, "JdbcDriverProbe", jdbc_url],
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise SystemExit(
            f"{jar_path}: DriverManager non trova il driver per {jdbc_url}\n"
            f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
        )
    return result.stdout.strip()


with tempfile.TemporaryDirectory(prefix="jdbc-probe-") as temp:
    probe_dir = Path(temp)
    source = probe_dir / "JdbcDriverProbe.java"
    source.write_text(PROBE_SOURCE, encoding="utf-8", newline="\n")
    subprocess.run(["javac", str(source)], check=True)

    for artifact, directory in ARTIFACTS.items():
        jar_path = find_jar(artifact, directory)
        with ZipFile(jar_path) as jar:
            providers = providers_from(jar)
            postgres = next(
                (provider for provider in providers
                 if provider.endswith(".postgresql.Driver")
                 or provider == "org.postgresql.Driver"),
                None,
            )
            if postgres is None:
                raise SystemExit(
                    f"{jar_path}: provider PostgreSQL assente: {providers}"
                )

            driver_class = postgres.replace(".", "/") + ".class"
            if driver_class not in jar.namelist():
                raise SystemExit(
                    f"{jar_path}: il provider {postgres} punta a una classe assente"
                )

            if artifact == "NetworkLanguage":
                if "org.sqlite.JDBC" not in providers:
                    raise SystemExit(
                        f"{jar_path}: provider SQLite assente: {providers}"
                    )
                if "org/sqlite/JDBC.class" not in jar.namelist():
                    raise SystemExit(
                        f"{jar_path}: classe SQLite dichiarata ma assente"
                    )

        postgres_driver = run_probe(
            jar_path, probe_dir, "jdbc:postgresql://127.0.0.1/test"
        )
        if not postgres_driver.endswith(".postgresql.Driver"):
            raise SystemExit(
                f"{jar_path}: driver PostgreSQL inatteso: {postgres_driver}"
            )

        if artifact == "NetworkLanguage":
            sqlite_driver = run_probe(
                jar_path, probe_dir, "jdbc:sqlite::memory:"
            )
            if sqlite_driver != "org.sqlite.JDBC":
                raise SystemExit(
                    f"{jar_path}: driver SQLite inatteso: {sqlite_driver}"
                )

        print(
            f"{artifact}: service descriptor e DriverManager validi "
            f"({', '.join(providers)})"
        )
