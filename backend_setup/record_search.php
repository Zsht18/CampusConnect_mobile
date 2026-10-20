<?php
// record_search.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$query_text = isset($_POST['query']) ? trim($_POST['query']) : '';

if(!empty($query_text)){
    $query = "INSERT INTO mobile_searches (query, search_count) VALUES (:query, 1) ON DUPLICATE KEY UPDATE search_count = search_count + 1";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(':query', $query_text);

    if($stmt->execute()){
        http_response_code(200);
        echo json_encode(array("success" => true, "message" => "Search recorded."));
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Unable to record search."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Query cannot be empty."));
}
?>