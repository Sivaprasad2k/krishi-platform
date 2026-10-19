def test_consult_stub_returns_contract_sample(client, auth_headers):
    payload = {
        "farmerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "query": "My tomato leaves are curling upwards and turning yellow at the margins. What should I do?",
        "language": "en",
        "cropContext": {
            "cropName": "Tomato",
            "cropVariety": "Arka Rakshak",
            "daysSincePlanting": 45,
            "currentGrowthStage": "Flowering",
        },
    }
    resp = client.post("/api/v1/ai/doctor/consult", headers=auth_headers, json=payload)
    assert resp.status_code == 200
    body = resp.json()
    assert body["success"] is True
    assert body["responseId"] == "ai-doc-19382"
    assert "recommendedActions" in body
    assert body["warningDisclaimer"]
    assert body["source"] == "ICAR Indian Institute of Horticultural Research"


def test_consult_requires_query(client, auth_headers):
    resp = client.post(
        "/api/v1/ai/doctor/consult",
        headers=auth_headers,
        json={"farmerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"},
    )
    assert resp.status_code == 400
    assert resp.json()["success"] is False
