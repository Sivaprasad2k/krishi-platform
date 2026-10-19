"""API contract conformance tests for POST /api/v1/ai/fertilizer/recommend.

Impact note (Phase 2, documented): this file previously pinned the *stub* sample body
literally (contract 3.3 example). The stub has been replaced by the deterministic
engine, so literal example values can no longer hold. Conformance now asserts the
contract shape (keys/types/enum categories), the exact values derived from the
verified TNAU rice dataset, the error envelope of section 4, and both hard gates
(UNSUPPORTED_CROP, MISSING_CALIBRATION_DATA). HTTP method, path, auth and response
field names are unchanged.
"""

import json

import pytest
from fastapi.testclient import TestClient


def post_recommend(client: TestClient, auth_headers, payload):
    return client.post("/api/v1/ai/fertilizer/recommend", headers=auth_headers, json=payload)


def post_raw_json(client: TestClient, auth_headers, payload):
    """Send a payload that contains NaN/Infinity literals (httpx json= refuses them)."""
    return client.post(
        "/api/v1/ai/fertilizer/recommend",
        headers={**auth_headers, "Content-Type": "application/json"},
        content=json.dumps(payload),  # json.dumps emits NaN/Infinity literals by default
    )


def contract_payload(**overrides):
    # The request example from contract 3.3 (Soil values are seed values for the engine).
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
    payload["soilTest"].update(overrides.get("soilTest", {}))
    overrides.pop("soilTest", None)
    payload.update(overrides)
    return payload


def test_success_shape_conforms_to_contract(client, auth_headers):
    resp = post_recommend(client, auth_headers, contract_payload())
    assert resp.status_code == 200
    body = resp.json()
    assert set(body) == {"success", "deficiencyStatus", "recommendedApplications", "guidanceSource"}
    assert body["success"] is True
    assert set(body["deficiencyStatus"]) == {"nitrogen", "phosphorus", "potassium"}
    for category in body["deficiencyStatus"].values():
        assert category in {"DEFICIENT", "MEDIUM", "OPTIMAL"}
    for application in body["recommendedApplications"]:
        assert set(application) == {"fertilizerType", "quantityKg", "splitStage", "method"}
        assert isinstance(application["quantityKg"], (int, float))
    assert "krishi-fertilizer-datasets-2.0.0" in body["guidanceSource"]


def test_success_exact_values_from_verified_dataset(client, auth_headers):
    resp = post_recommend(client, auth_headers, contract_payload())
    body = resp.json()
    # Hand-computed for the TNAU rice fixture (see test_fertilizer_service.py header).
    assert body["deficiencyStatus"] == {
        "nitrogen": "MEDIUM",       # 300 in [240, 480]
        "phosphorus": "DEFICIENT",  # 10 < 11
        "potassium": "OPTIMAL",     # 300 > 280
    }
    apps = body["recommendedApplications"]
    urea = [a for a in apps if a["fertilizerType"] == "UREA"]
    ssp = [a for a in apps if a["fertilizerType"] == "SINGLE_SUPER_PHOSPHATE"]
    mop = [a for a in apps if a["fertilizerType"] == "MURIATE_OF_POTASH"]
    assert [(a["splitStage"], a["quantityKg"]) for a in urea] == [
        ("Basal", 65.99), ("Tillering", 65.98), ("Panicle Initiation", 65.98), ("Heading", 65.98)
    ]
    assert [(a["splitStage"], a["quantityKg"]) for a in ssp] == [("Basal", 252.93)]
    assert [(a["splitStage"], a["quantityKg"]) for a in mop] == [
        ("Basal", 16.87), ("Tillering", 16.86), ("Panicle Initiation", 16.86), ("Heading", 16.86)
    ]
    # Every application carries a non-empty method instruction.
    assert all(a["method"] for a in apps)


def test_identical_requests_are_deterministic(client, auth_headers):
    first = post_recommend(client, auth_headers, contract_payload()).json()
    second = post_recommend(client, auth_headers, contract_payload()).json()
    assert first == second


def test_missing_internal_key_is_unauthorized_with_error_envelope(client):
    resp = client.post("/api/v1/ai/fertilizer/recommend", json=contract_payload())
    assert resp.status_code == 401
    body = resp.json()
    assert body == {
        "success": False,
        "errorCode": "UNAUTHORIZED",
        "message": "Missing or invalid X-Krishi-Internal-Key header.",
    }


def test_unsupported_crop_rejected_explicitly(client, auth_headers):
    resp = post_recommend(client, auth_headers, contract_payload(cropName="Maize"))
    assert resp.status_code == 400
    body = resp.json()
    assert body["success"] is False
    assert body["errorCode"] == "UNSUPPORTED_CROP"
    assert "Maize" in body["message"]


def test_bare_alias_not_silently_mapped(client, auth_headers):
    resp = post_recommend(client, auth_headers, contract_payload(cropName="paddy"))
    assert resp.status_code == 400
    assert resp.json()["errorCode"] == "UNSUPPORTED_CROP"


def test_blank_crop_name_is_validation_error(client, auth_headers):
    resp = post_recommend(client, auth_headers, contract_payload(cropName="   "))
    assert resp.status_code == 400
    assert resp.json()["errorCode"] == "VALIDATION_ERROR"


def test_wheat_success_conformance_with_tnau_schedule(client, auth_headers):
    """Wheat graduated to a verified dataset in v2.0.0 (TNAU blanket 80:40:40)."""
    resp = post_recommend(client, auth_headers, contract_payload(cropName="Wheat"))
    assert resp.status_code == 200
    body = resp.json()
    assert body["success"] is True
    uapp = [a for a in body["recommendedApplications"] if a["fertilizerType"] == "UREA"]
    assert [(a["splitStage"], a["quantityKg"]) for a in uapp] == [
        ("Basal", 87.97),
        ("Crown Root Initiation", 87.98),
    ]
    assert body["guidanceSource"].endswith("krishi-fertilizer-datasets-2.0.0")


def test_tomato_reports_missing_calibration(client, auth_headers):
    resp = post_recommend(client, auth_headers, contract_payload(cropName="Tomato"))
    assert resp.status_code == 501
    body = resp.json()
    assert body["success"] is False
    assert body["errorCode"] == "MISSING_CALIBRATION_DATA"
    assert "Tomato" in body["message"]


@pytest.mark.parametrize(
    "bad_soil",
    [
        {"nitrogenKgPerHa": -1.0},           # negative concentration
        {"phosphorusKgPerHa": -0.001},       # negative concentration
        {"potassiumKgPerHa": -2.0},
        {"ph": -0.01},                       # outside technically valid pH scale
        {"ph": 14.5},
        {"organicCarbonPercent": 101.0},     # percentage scale, not fraction
        {"nitrogenKgPerHa": float("nan")},
        {"potassiumKgPerHa": float("inf")},
        {"nitrogenKgPerHa": float("-inf")},
    ],
)
def test_invalid_soil_values_rejected_with_validation_envelope(client, auth_headers, bad_soil):
    resp = post_raw_json(client, auth_headers, contract_payload(soilTest=bad_soil))
    assert resp.status_code == 400, bad_soil
    body = resp.json()
    assert body["success"] is False
    assert body["errorCode"] == "VALIDATION_ERROR"


def test_nonpositive_area_rejected(client, auth_headers):
    resp = post_recommend(client, auth_headers, contract_payload(fieldAreaAcres=0.0))
    assert resp.status_code == 400
    assert resp.json()["errorCode"] == "VALIDATION_ERROR"


def test_missing_soil_test_rejected(client, auth_headers):
    payload = contract_payload()
    del payload["soilTest"]
    resp = post_recommend(client, auth_headers, payload)
    assert resp.status_code == 400
    assert resp.json()["errorCode"] == "VALIDATION_ERROR"


def test_organic_carbon_fraction_lookalike_is_not_a_special_case(client, auth_headers):
    # 0.45 as a percentage is a plausible (low) organic carbon reading; the field name
    # fixes the basis and no silent x100 conversion is attempted (documented in schema).
    resp = post_recommend(client, auth_headers, contract_payload())
    assert resp.status_code == 200


def test_no_stack_traces_in_error_bodies(client, auth_headers):
    for payload, headers in (
        (contract_payload(cropName="Maize"), auth_headers),
        (contract_payload(soilTest={"nitrogenKgPerHa": -5.0}), auth_headers),
    ):
        resp = post_recommend(client, headers, payload)
        text = resp.text
        assert "Traceback" not in text
        assert 'File "<string>"' not in text
