variable "project" {
  type    = string
  default = "lineup"
}

variable "region" {
  type    = string
  default = "ap-southeast-1"
}

variable "api_instance_type" {
  type    = string
  default = "t4g.small"
}

variable "spring_profiles" {
  type    = string
  default = "prod"
}

variable "auth0_domain" {
  type = string
}

variable "auth0_audience" {
  type = string
}

variable "stripe_secret_key" {
  type      = string
  sensitive = true
}

variable "stripe_webhook_secret" {
  type      = string
  sensitive = true
}

variable "frontend_url" {
  type    = string
  default = "http://localhost:5173"
}

variable "loadtest_runner_enabled" {
  type    = bool
  default = false
}

variable "loadtest_runner_instance_type" {
  type    = string
  default = "c7g.2xlarge"
}