resource "aws_dynamodb_table" "seats" {
  name         = "${var.project}-seats"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "eventId"
    type = "S"
  }

  attribute {
    name = "eventSection"
    type = "S"
  }

  attribute {
    name = "status"
    type = "S"
  }

  global_secondary_index {
    name            = "EventStatusIndex"
    projection_type = "ALL"

    key_schema {
      attribute_name = "eventId"
      key_type       = "HASH"
    }

    key_schema {
      attribute_name = "status"
      key_type       = "RANGE"
    }
  }

  global_secondary_index {
    name            = "EventSectionIndex"
    projection_type = "ALL"

    key_schema {
      attribute_name = "eventSection"
      key_type       = "HASH"
    }

    key_schema {
      attribute_name = "status"
      key_type       = "RANGE"
    }
  }
}