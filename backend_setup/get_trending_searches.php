<?php
// get_trending_searches.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");

include_once 'db_connect.php';

$query = "SELECT query, search_count FROM mobile_searches ORDER BY search_count DESC, last_searched_at DESC LIMIT 4";
$stmt = $conn->prepare($query);
$stmt->execute();

$searches = $stmt->fetchAll(PDO::FETCH_ASSOC);

http_response_code(200);
echo json_encode($searches);
?>