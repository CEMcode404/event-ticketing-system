output "db_endpoint" {
  value = aws_db_instance.main.address
}

output "redis_endpoint" {
  value = aws_elasticache_replication_group.main.primary_endpoint_address
}

output "api_instance_id" {
  value = aws_instance.api.id
}

output "artifact_bucket" {
  value = aws_s3_bucket.artifacts.id
}