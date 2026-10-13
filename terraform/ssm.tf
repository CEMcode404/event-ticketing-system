locals {
  app_config = {
    "db/host"             = aws_db_instance.main.address
    "db/port"             = tostring(aws_db_instance.main.port)
    "db/name"             = aws_db_instance.main.db_name
    "db/username"         = aws_db_instance.main.username
    "redis/host"          = aws_elasticache_replication_group.main.primary_endpoint_address
    "redis/port"          = tostring(aws_elasticache_replication_group.main.port)
    "auth0/domain"        = var.auth0_domain
    "auth0/audience"      = var.auth0_audience
    "app/frontend-url"    = var.frontend_url
    "cors/allowed-origin" = var.frontend_url
  }

  app_parameter_names = concat(
    keys(local.app_config),
    ["db/password", "stripe/secret-key", "stripe/webhook-secret"]
  )
}

resource "aws_ssm_parameter" "app_config" {
  for_each = local.app_config

  name  = "/${var.project}/${each.key}"
  type  = "String"
  value = each.value
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