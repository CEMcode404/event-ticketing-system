locals {
  app_url = "https://${aws_cloudfront_distribution.main.domain_name}"

  app_config = {
    "db/host"        = aws_db_instance.main.address
    "db/port"        = tostring(aws_db_instance.main.port)
    "db/name"        = aws_db_instance.main.db_name
    "db/username"    = aws_db_instance.main.username
    "redis/host"     = aws_elasticache_replication_group.main.primary_endpoint_address
    "redis/port"     = tostring(aws_elasticache_replication_group.main.port)
    "auth0/domain"   = var.auth0_domain
    "auth0/audience" = var.auth0_audience
  }

  app_parameter_names = sort(concat(
    keys(local.app_config),
    ["app/frontend-url", "cors/allowed-origin", "db/password", "stripe/secret-key", "stripe/webhook-secret"]
  ))
}

resource "aws_ssm_parameter" "app_config" {
  for_each = local.app_config

  name  = "/${var.project}/${each.key}"
  type  = "String"
  value = each.value
}

resource "aws_ssm_parameter" "frontend_url" {
  name  = "/${var.project}/app/frontend-url"
  type  = "String"
  value = local.app_url
}

resource "aws_ssm_parameter" "cors_allowed_origin" {
  name  = "/${var.project}/cors/allowed-origin"
  type  = "String"
  value = local.app_url
}

moved {
  from = aws_ssm_parameter.app_config["app/frontend-url"]
  to   = aws_ssm_parameter.frontend_url
}

moved {
  from = aws_ssm_parameter.app_config["cors/allowed-origin"]
  to   = aws_ssm_parameter.cors_allowed_origin
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/${var.project}/db/password"
  type  = "SecureString"
  value = random_password.db.result
}

resource "aws_ssm_parameter" "stripe_secret_key" {
  name  = "/${var.project}/stripe/secret-key"
  type  = "SecureString"
  value = var.stripe_secret_key
}

resource "aws_ssm_parameter" "stripe_webhook_secret" {
  name  = "/${var.project}/stripe/webhook-secret"
  type  = "SecureString"
  value = var.stripe_webhook_secret
}