<?php
// get_comments.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");

include_once 'db_connect.php';

$post_id = isset($_GET['post_id']) ? intval($_GET['post_id']) : 0;

if($post_id > 0){
    $query = "SELECT c.id, c.post_id, c.user_id, c.content, c.likes, c.created_at, u.username as author_name
              FROM mobile_comments c
              LEFT JOIN mobile_users u ON c.user_id = u.id
              WHERE c.post_id = :post_id
              ORDER BY c.created_at ASC";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(':post_id', $post_id);
    $stmt->execute();
    $comments = $stmt->fetchAll(PDO::FETCH_ASSOC);

    http_response_code(200);
    echo json_encode($comments);
} else {
    http_response_code(200);
    echo json_encode(array());
}
?>