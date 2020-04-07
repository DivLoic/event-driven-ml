
output "ksql-server" {
  value = "http://${module.ksql.ksql_server_address}"
}