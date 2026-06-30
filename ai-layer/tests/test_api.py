"""API tests via FastAPI TestClient — verifies the HTTP surface of the ai-layer (no network)."""
import unittest

from fastapi.testclient import TestClient

from main import app


class ApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(app)

    def test_health(self):
        r = self.client.get("/health")
        self.assertEqual(r.status_code, 200)
        body = r.json()
        self.assertEqual(body["status"], "ok")
        self.assertIn("embedder", body)
        self.assertIn("rag", body)

    def test_embed(self):
        r = self.client.post("/embed", json={"text": "deploy the frontend"})
        self.assertEqual(r.status_code, 200)
        body = r.json()
        self.assertGreater(body["dim"], 0)
        self.assertEqual(len(body["vector"]), body["dim"])

    def test_rag_ingest_then_query(self):
        ing = self.client.post("/rag/ingest", json={
            "text": "To deploy the frontend run npm run build then netlify deploy --prod.",
            "source": "doc",
        })
        self.assertEqual(ing.status_code, 200)
        self.assertGreaterEqual(ing.json()["ingested_chunks"], 1)

        q = self.client.post("/rag/query", json={"query": "how to deploy frontend", "topK": 3})
        self.assertEqual(q.status_code, 200)
        results = q.json()["results"]
        self.assertTrue(results)
        self.assertIn("netlify", results[0]["text"].lower())

    def test_skills_intake_text(self):
        r = self.client.post("/skills/intake", json={
            "name": "Backup DB",
            "text": "Run pg_dump to back up. Store the file off-machine.",
        })
        self.assertEqual(r.status_code, 200)
        body = r.json()
        self.assertEqual(body["format"], "text")
        self.assertGreaterEqual(body["chunk_count"], 1)

    def test_psychology_intent(self):
        r = self.client.post("/psychology/intent", json={"text": "buy 10 AAPL"})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.json()["intent"], "TRADE_LIVE")

    def test_psychology_priority(self):
        r = self.client.post("/psychology/priority", json={"text": "URGENT fix now"})
        self.assertEqual(r.status_code, 200)
        self.assertGreater(r.json()["score"], 0.3)

    def test_nlp_summarize(self):
        text = " ".join("Sentence %d about indexing." % i for i in range(8))
        r = self.client.post("/nlp/summarize", json={"text": text, "maxSentences": 2})
        self.assertEqual(r.status_code, 200)
        self.assertTrue(r.json()["summary"])

    def test_anomaly_detect(self):
        r = self.client.post("/anomaly/detect", json={"series": [10, 11, 9, 10, 200, 10, 11]})
        self.assertEqual(r.status_code, 200)
        self.assertTrue(any(a["value"] == 200 for a in r.json()["anomalies"]))

    def test_api_key_enforced_when_set(self):
        import os
        os.environ["ULTRON_PYTHON_BRIDGE_KEY"] = "secret123"
        try:
            denied = self.client.post("/embed", json={"text": "x"})
            self.assertEqual(denied.status_code, 401)
            ok = self.client.post("/embed", json={"text": "x"}, headers={"X-API-Key": "secret123"})
            self.assertEqual(ok.status_code, 200)
        finally:
            del os.environ["ULTRON_PYTHON_BRIDGE_KEY"]


if __name__ == "__main__":
    unittest.main()
