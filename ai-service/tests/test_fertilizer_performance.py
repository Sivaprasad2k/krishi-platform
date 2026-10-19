"""SLA evidence for the fertilizer endpoint (contract budget: 4000 ms).

Reproducible measurement, not a target gate: prints mean/p50/max for (a) the pure
engine calculation and (b) full endpoint latency over localhost, and uses very loose
sanity assertions far below the SLA so CI stays stable.
"""

import statistics
import time

from fastapi.testclient import TestClient

from app.services import fertilizer as engine

SOIL = dict(nitrogen=300.0, phosphorus=10.0, potassium=300.0, ph=6.8, organic_carbon=0.45)
ENGINE_REPS = 200
ENDPOINT_REPS = 15


def test_pure_calculation_latency(capsys):
    durations = []
    for _ in range(ENGINE_REPS):
        start = time.perf_counter()
        engine.compute_recommendation(
            crop_name="Rice (Paddy)",
            field_area_acres=2.5,
            soil_nitrogen=SOIL["nitrogen"],
            soil_phosphorus=SOIL["phosphorus"],
            soil_potassium=SOIL["potassium"],
            ph=SOIL["ph"],
            organic_carbon_percent=SOIL["organic_carbon"],
        )
        durations.append((time.perf_counter() - start) * 1000.0)
    mean_ms = statistics.fmean(durations)
    capsys.readouterr()  # discard accidental prints from the engine
    print(f"[fertilizer] pure engine: reps={ENGINE_REPS} mean={mean_ms:.3f} ms max={max(durations):.3f} ms")
    assert mean_ms < 50.0, "pure calculation unexpectedly slow (documented sanity guard)"


def test_endpoint_latency(client: TestClient, auth_headers, capsys):
    payload = {
        "cropName": "Rice (Paddy)",
        "fieldAreaAcres": 2.5,
        "soilTest": {
            "nitrogenKgPerHa": 300.0,
            "phosphorusKgPerHa": 10.0,
            "potassiumKgPerHa": 300.0,
            "ph": 6.8,
            "organicCarbonPercent": 0.45,
        },
        "growthStage": "Tillering",
    }
    durations = []
    bodies = []
    for _ in range(ENDPOINT_REPS):
        start = time.perf_counter()
        resp = client.post("/api/v1/ai/fertilizer/recommend", headers=auth_headers, json=payload)
        durations.append((time.perf_counter() - start) * 1000.0)
        assert resp.status_code == 200
        bodies.append(resp.json())
    # Every response identical: latency measurement did not perturb determinism.
    assert all(body == bodies[0] for body in bodies)
    mean_ms = statistics.fmean(durations)
    print(f"[fertilizer] endpoint: reps={ENDPOINT_REPS} mean={mean_ms:.3f} ms max={max(durations):.3f} ms")
    assert max(durations) < 1000.0, "endpoint call unexpectedly slow (sanity guard)"
