CONTRACT_SOIL_TEST = {
    "nitrogenKgPerHa": 180.0,
    "phosphorusKgPerHa": 14.0,
    "potassiumKgPerHa": 210.0,
    "ph": 6.8,
    "organicCarbonPercent": 0.45,
}


def test_recommend_stub_returns_contract_sample(client, auth_headers):
    payload = {
        "cropName": "Rice (Paddy)",
        "fieldAreaAcres": 2.5,
        "soilTest": CONTRACT_SOIL_TEST,
        "growthStage": "Tillering",
    }
    resp = client.post("/api/v1/ai/fertilizer/recommend", headers=auth_headers, json=payload)
    assert resp.status_code == 200
    body = resp.json()
    assert body["success"] is True
    assert body["deficiencyStatus"] == {
        "nitrogen": "DEFICIENT",
        "phosphorus": "MEDIUM",
        "potassium": "OPTIMAL",
    }
    assert body["recommendedApplications"][0]["fertilizerType"] == "UREA"
    assert body["guidanceSource"] == "ICAR Nutrient Management Guidelines"


def test_recommend_rejects_invalid_soil_ph(client, auth_headers):
    bad_soil = {**CONTRACT_SOIL_TEST, "ph": 30.0}
    resp = client.post(
        "/api/v1/ai/fertilizer/recommend",
        headers=auth_headers,
        json={"cropName": "Rice (Paddy)", "fieldAreaAcres": 2.5, "soilTest": bad_soil},
    )
    assert resp.status_code == 400
    assert resp.json()["success"] is False
