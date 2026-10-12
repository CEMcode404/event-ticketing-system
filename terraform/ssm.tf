locals {
  app_config = {
    "db/host"     = aws_db_instance.main.address
    "db/port"     = tostring(aws_db_instance.main.port)
    "db/name"     = aws_db_instance.main.db_name
    "db/username" = aws_db_instance.main.username
    "redis/host"  = aws_elasticache_replication_group.main.primary_endpoint_address
    "redis/port"  = tostring(aws_elasticache_replication_group.main.port)
  }
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