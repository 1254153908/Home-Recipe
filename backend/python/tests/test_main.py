from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health():
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


def test_recognize_tomato():
    resp = client.post("/api/recognize", json={
        "sourceType": "link",
        "content": "https://example.com/tomato-egg-recipe",
    })
    assert resp.status_code == 200
    data = resp.json()
    assert data["title"] == "Tomato Egg Stir-fry"
    assert len(data["steps"]) == 4
    assert len(data["ingredients"]) == 3
    assert len(data["seasonings"]) == 3


def test_recognize_tofu():
    resp = client.post("/api/recognize", json={
        "sourceType": "video",
        "content": "How to make mapo tofu at home",
    })
    assert resp.status_code == 200
    data = resp.json()
    assert data["title"] == "Mapo Tofu"
    assert len(data["steps"]) == 4


def test_recognize_ribs():
    resp = client.post("/api/recognize", json={
        "sourceType": "link",
        "content": "https://cooking.com/sweet-and-sour-ribs",
    })
    assert resp.status_code == 200
    data = resp.json()
    assert data["title"] == "Sweet and Sour Ribs"


def test_recognize_default():
    resp = client.post("/api/recognize", json={
        "sourceType": "image",
        "content": "https://unknown.com/some-random-dish",
    })
    assert resp.status_code == 200
    data = resp.json()
    assert data["title"] == "Tomato Egg Stir-fry"
