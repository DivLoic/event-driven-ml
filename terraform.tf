provider "google" {
  credentials = "${file(".terraform/edml-gcp-access-key.json")}"
  project = "event-driven-ml"
  region = "europe-west1"
}

resource "google_storage_bucket" "source-bucket" {
  name = "edml_data"
  location = "EU"
  labels = {
    env = "prod"
  }
}

resource "google_storage_bucket" "explo-bucket" {
  name = "edml_interactive"
  location = "EU"
  labels = {
    env = "dev"
  }
}

resource "google_bigquery_dataset" "edml-dataset" {
  dataset_id = "edml_dataset"
  friendly_name = "test"
  description = "Dataset containing taxi trip historical data"
  location = "EU"

  labels = {
    env = "prod"
  }
}

resource "google_bigquery_table" "edml-trip-history" {
  dataset_id = "${google_bigquery_dataset.edml-dataset.dataset_id}"
  table_id = "trip_history"

  time_partitioning {
    type = "DAY"
  }

  labels = {
    env = "prod"
  }
}