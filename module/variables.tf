
variable "aws_region" {
  type        = string
  default     = "eu-west-1"
  description = "Region where to provision Lambda"
}


variable "bucket_name"{
    type        = string
    default     = "ash1g13.lambda.bucket.test.com"
    description = "Name of the bucket to save the lambda function"
}



variable "lambda_api_name" {
  type        = string
  default     = "lambda_api_ws"
  description = "Name of API Gateway for Lambda Websocket"
}


variable "api_post_http_method" {
  type        = string
  default     = "POST"
  description = "Method to trigger Lambda through API Gateway"
}

variable "api_get_http_method" {
  type        = string
  default     = "GET"
  description = "Method to trigger Lambda through API Gateway"
}

variable "api_delete_http_method" {
  type        = string
  default     = "DELETE"
  description = "Method to trigger Lambda through API Gateway"
}


variable "api_resource_path" {
  type        = string
  default     = "websocket"
  description = "API Gateway Path"
}

variable "api_stage_name" {
  type        = string
  default     = "dev"
  description = "API Gateway Stage"
}


