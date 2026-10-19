def test_health_matches_contract(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "UP"
    assert body["service"] == "krishi-ai-service"
    assert body["timestamp"].endswith("Z")


def test_health_does_not_require_auth(client):
    resp = client.get("/health")
    assert resp.status_code == 200
