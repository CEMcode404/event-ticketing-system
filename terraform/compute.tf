data "aws_ami" "al2023_arm" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-kernel-*-arm64"]
  }
}

resource "aws_instance" "api" {
  ami                    = data.aws_ami.al2023_arm.id
  instance_type          = var.api_instance_type
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.api.id]
  iam_instance_profile   = aws_iam_instance_profile.api.name

  user_data = templatefile("${path.module}/user-data.sh.tftpl", {
    project         = var.project
    region          = var.region
    artifact_bucket = aws_s3_bucket.artifacts.id
    parameter_names = join(" ", local.app_parameter_names)
    spring_profiles = var.spring_profiles
  })
  user_data_replace_on_change = true

  metadata_options {
    http_tokens = "required"
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 10
    encrypted   = true
  }

  tags = { Name = "${var.project}-api" }

  depends_on = [
    aws_ssm_parameter.app_config,
    aws_ssm_parameter.db_password,
    aws_ssm_parameter.stripe_secret_key,
    aws_ssm_parameter.stripe_webhook_secret,
  ]

  lifecycle {
    ignore_changes = [ami]
  }
}