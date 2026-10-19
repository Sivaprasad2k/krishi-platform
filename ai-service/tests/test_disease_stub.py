"""Pins the Phase-1 stub endpoints to the exact contract section-3 sample responses."""

from fastapi.testclient import TestClient


def diagnose_payload(**overrides):
    data = {"cropName": "Rice (Paddy)", "cropVariety": "Samba Masoori", "growthStage": "Tillering"}
    data.update(overrides)
    return data


def send_diagnose(client: TestClient, auth_headers, files=None, data=None):
    files = files or {"image": ("leaf.jpg", b"stub-jpeg-bytes", "image/jpeg")}
    return client.post(
        "/api/v1/ai/disease-detection/diagnose",
        headers=auth_headers,
        files=files,
        data=data if data is not None else diagnose_payload(),
    )


def test_diagnose_stub_returns_contract_sample(client, auth_headers):
    resp = send_diagnose(client, auth_headers)
    assert resp.status_code == 200
    body = resp.json()
    assert body["success"] is True
    assert body["diagnosisId"] == "ai-diag-88492024"
    assert body["diseaseDetected"] is True
    assert body["diseaseCode"] == "RICE_BLAST"
    assert body["diseaseName"] == "Blast Disease (Magnaporthe oryzae)"
    assert body["confidence"] == 0.94
    assert body["severity"] == "HIGH"
    assert body["recommendedTreatment"]["packageSource"] == "ICAR Directorate of Rice Research"


def test_diagnose_accepts_optional_fields(client, auth_headers):
    resp = send_diagnose(client, auth_headers, data=diagnose_payload(observedSymptoms="lesions on leaves"))
    assert resp.status_code == 200


def test_diagnose_rejects_unsupported_image_type(client, auth_headers):
    resp = send_diagnose(
        client, auth_headers, files={"image": ("notes.txt", b"not-an-image", "text/plain")}
    )
    assert resp.status_code == 400
    body = resp.json()
    assert body == {
        "success": False,
        "errorCode": "INVALID_IMAGE_PAYLOAD",
        "message": "Only JPEG or PNG foliage images are accepted.",
    }


def test_diagnose_rejects_oversized_image(client, auth_headers):
    oversized = b"x" * (10 * 1024 * 1024 + 1)
    resp = send_diagnose(client, auth_headers, files={"image": ("leaf.jpg", oversized, "image/jpeg")})
    assert resp.status_code == 400
    assert resp.json()["errorCode"] == "INVALID_IMAGE_PAYLOAD"


def test_diagnose_requires_crop_name(client, auth_headers):
    resp = send_diagnose(client, auth_headers, data={})
    assert resp.status_code == 400
    assert resp.json()["success"] is False


def test_diagnose_requires_internal_key(client):
    files = {"image": ("leaf.jpg", b"stub-jpeg-bytes", "image/jpeg")}
    resp = client.post(
        "/api/v1/ai/disease-detection/diagnose",
        files=files,
        data=diagnose_payload(),
    )
    assert resp.status_code == 401
    body = resp.json()
    assert body["success"] is False
    assert body["errorCode"] == "UNAUTHORIZED"
