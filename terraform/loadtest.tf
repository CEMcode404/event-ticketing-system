resource "aws_security_group" "loadtest" {
  count = var.loadtest_runner_enabled ? 1 : 0

  name        = "${var.project}-loadtest"
  description = "k6 load test runner"
  vpc_id      = aws_vpc.main.id
}

resource "aws_vpc_security_group_egress_rule" "loadtest_all" {
  count = var.loadtest_runner_enabled ? 1 : 0

  security_group_id = aws_security_group.loadtest[0].id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_vpc_security_group_ingress_rule" "api_from_loadtest" {
  count = var.loadtest_runner_enabled ? 1 : 0

  security_group_id            = aws_security_group.api.id
  referenced_security_group_id = aws_security_group.loadtest[0].id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "db_from_loadtest" {
  count = var.loadtest_runner_enabled ? 1 : 0

  security_group_id            = aws_security_group.db.id
  referenced_security_group_id = aws_security_group.loadtest[0].id
  from_port                    = 3306
  to_port                      = 3306
  ip_protocol                  = "tcp"
}

data "aws_iam_policy_document" "loadtest" {
  statement {
    sid       = "ReadSeatsForReconcile"
    actions   = ["dynamodb:Query"]
    resources = ["${aws_dynamodb_table.seats.arn}/index/*"]
  }

  statement {
    sid       = "ReadDbConfigForReconcile"
    actions   = ["ssm:GetParameter"]
    resources = ["arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/${var.project}/db/*"]
  }

  statement {
    sid       = "DecryptDbPassword"
    actions   = ["kms:Decrypt"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["ssm.${var.region}.amazonaws.com"]
    }
  }

  statement {
    sid       = "DownloadScripts"
    actions   = ["s3:GetObject", "s3:ListBucket"]
    resources = [aws_s3_bucket.artifacts.arn, "${aws_s3_bucket.artifacts.arn}/*"]
  }

  statement {
    sid = "SessionManager"
    actions = [
      "ssm:UpdateInstanceInformation",
      "ssmmessages:CreateControlChannel",
      "ssmmessages:CreateDataChannel",
      "ssmmessages:OpenControlChannel",
      "ssmmessages:OpenDataChannel",
      "ec2messages:*",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role" "loadtest" {
  count = var.loadtest_runner_enabled ? 1 : 0

  name                 = "${var.project}-loadtest-role"
  assume_role_policy   = data.aws_iam_policy_document.ec2_assume_role.json
  permissions_boundary = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:policy/${var.project}-workload-boundary"
}

resource "aws_iam_role_policy" "loadtest" {
  count = var.loadtest_runner_enabled ? 1 : 0

  name   = "${var.project}-loadtest"
  role   = aws_iam_role.loadtest[0].id
  policy = data.aws_iam_policy_document.loadtest.json
}

resource "aws_iam_instance_profile" "loadtest" {
  count = var.loadtest_runner_enabled ? 1 : 0

  name = "${var.project}-loadtest-profile"
  role = aws_iam_role.loadtest[0].name
}

resource "aws_instance" "loadtest" {
  count = var.loadtest_runner_enabled ? 1 : 0

  ami                    = data.aws_ami.al2023_arm.id
  instance_type          = var.loadtest_runner_instance_type
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.loadtest[0].id]
  iam_instance_profile   = aws_iam_instance_profile.loadtest[0].name
  user_data              = file("${path.module}/loadtest-runner.sh")

  metadata_options {
    http_tokens = "required"
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 20
    encrypted   = true
  }

  tags = { Name = "${var.project}-loadtest" }

  lifecycle {
    ignore_changes = [ami]
  }
}