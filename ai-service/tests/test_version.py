def test_version_requires_internal_key(client):
    resp = client.get("/api/v1/ai/version")
    assert resp.status_code == 401


def test_version_rejects_wrong_key(client):
    resp = client.get("/api/v1/ai/version", headers={"X-Krishi-Internal-Key": "wrong-key"})
    assert resp.status_code == 401


def test_version_matches_contract(client, auth_headers):
    resp = client.get("/api/v1/ai/version", headers=auth_headers)
    assert resp.status_code == 200
    body = resp.json()
    assert body["serviceVersion"] == "1.0.0"
    assert body["diseaseModelVersion"] == "plant-vision-v2.1"
    assert body["doctorModel"] == "agri-llm-lora-v1"
    assert body["supportedCrops"] == ["Rice (Paddy)", "Tomato", "Wheat"]
