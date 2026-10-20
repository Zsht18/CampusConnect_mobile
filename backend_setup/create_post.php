<?php
// create_post.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$title = isset($_POST['title']) ? trim($_POST['title']) : '';
$category = isset($_POST['category']) ? trim($_POST['category']) : '';
$content = isset($_POST['content']) ? trim($_POST['content']) : '';
$is_anonymous = isset($_POST['is_anonymous']) ? intval($_POST['is_anonymous']) : 0;

if($user_id > 0 && !empty($title) && !empty($content)){
    $query = "INSERT INTO mobile_posts (user_id, title, category, content, is_anonymous) VALUES (:user_id, :title, :category, :content, :is_anonymous)";
    $stmt = $conn->prepare($query);

    $stmt->bindParam(':user_id', $user_id);
    $stmt->bindParam(':title', $title);
    $stmt->bindParam(':category', $category);
    $stmt->bindParam(':content', $content);
    $stmt->bindParam(':is_anonymous', $is_anonymous);

    if($stmt->execute()){
        http_response_code(200);
        echo json_encode(array("success" => true, "message" => "Post created successfully."));
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Unable to create post."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Incomplete post data. Please enter Title and Content."));
}
?>