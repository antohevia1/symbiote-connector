data "aws_vpc" "default" {
  default = true
}
module "ec2_sg" {
  source = "terraform-aws-modules/security-group/aws"

  name        = "ec2_websocket_sg"
  description = "Security group for ec2_websocket_sg"
  vpc_id      = data.aws_vpc.default.id

  ingress_cidr_blocks = ["0.0.0.0/0"]
  ingress_rules       = ["ssh-tcp"]
  egress_rules        = ["all-all"]
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-kernel-*-x86_64-gp2"]
  }
}


resource "aws_key_pair" "asanchez" {
  key_name   = "asanchez"
  public_key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCPpQ6eVVKFsXn8gjz8+EBaBm76A94N+Qh32y1+GIs7uje0kdf+gXpdsezE5gketGwIz+KjehY1qKpf5SHAH2i5gSMU2rgutIkMkaN8rSAzyAKnWnnoVyzho2RFvsvbCH/RGs3mAbiDa1pBFlTAIYbQsBRar28XzyFFmQ+GUD9jMdAWV/D6Vbbpq/AQxgaNYtWJ6QynJg/8ueWCxiFxHBhxZv16ZVthDWo0AlyhCNUxr5q1rX/9ekwh93vcnxyjyu5Z6XRn9bwGesDO0QSJbucgIoM//D+S5QTMEw10ndC1ddUYHhIHf6lZXNR8aF2v61a8PRZCfycUMxRQCFkzm9b9"
}



resource "aws_iam_role" "ec2_role_ssm" {
  name = "ec2_role_ssm"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF

  tags = {
    project = "ws-connection"
  }
}

resource "aws_iam_instance_profile" "ec2_ssm_profile" {
  name = "ec2_ssm_profile"
  role = aws_iam_role.ec2_role_ssm.name
}


data "aws_iam_policy" "EC2SSMFullAccess" {
  arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachment" "test-attach" {
  role       = aws_iam_role.ec2_role_ssm.name
  policy_arn = data.aws_iam_policy.EC2SSMFullAccess.arn
}

data "template_file" "user_data" {
  template = file("scripts/prepare-docker.yaml")
}

resource "aws_instance" "ws_instance" {
  ami           = data.aws_ami.amazon_linux.id
  instance_type = "t2.xlarge"

  root_block_device {
    volume_size = 32
  }

  user_data = data.template_file.user_data.rendered

  vpc_security_group_ids = [
  module.ec2_sg.security_group_id]

  iam_instance_profile = aws_iam_instance_profile.ec2_ssm_profile.name
  key_name = "asanchez"



  tags = {
    Name    = "ws_instance"
    project = "ws-connection"
  }

  monitoring              = false
  disable_api_termination = false
}




#Lambda definition

data "archive_file" "lambda_ws" {
  type = "zip"

  source_dir  = "${path.module}/lambda"
  output_path = "${path.module}/lambda.zip"
}

resource "aws_s3_bucket" "lambda_bucket" {
  bucket = "ash1g13.lambda.bucket.test.com"
}

resource "aws_s3_bucket_object" "lambda_ws" {
  bucket = aws_s3_bucket.lambda_bucket.id

  key    = "lambda.zip"
  source = data.archive_file.lambda_ws.output_path

  etag = filemd5(data.archive_file.lambda_ws.output_path)
}

resource "aws_iam_role" "lambda_exec" {
  name = "serverless_lambda"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Sid    = ""
      Principal = {
        Service = "lambda.amazonaws.com"
      }
      }
    ]
  })
}


resource "aws_iam_role_policy_attachment" "role-policy-attachment" {
  for_each = toset([
   "arn:aws:iam::aws:policy/AmazonSSMFullAccess", 
   "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
  ])

  role       = aws_iam_role.lambda_exec.name
  policy_arn = each.value
}

resource "aws_lambda_function" "ws_api_lambda" {
  function_name = "ws_api_lambda"

  s3_bucket = aws_s3_bucket.lambda_bucket.id
  s3_key    = aws_s3_bucket_object.lambda_ws.key

  runtime = "python3.9"
  handler = "lambda_function.lambda_handler"

  source_code_hash = data.archive_file.lambda_ws.output_base64sha256

  role = aws_iam_role.lambda_exec.arn
  timeout = 22

  environment {
    variables = {
      INSTANCE_ID = aws_instance.ws_instance.arn
    }
  }
}
