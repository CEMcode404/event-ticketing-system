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

output "app_url" {
  value = local.app_url
}

output "cloudfront_distribution_id" {
  value = aws_cloudfront_distribution.main.id
}

output "frontend_bucket" {
  value = aws_s3_bucket.frontend.id
}