<?php
// add_comment.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';
require_once __DIR__ . '/MobileAiModeration.php';

$post_id = isset($_POST['post_id']) ? intval($_POST['post_id']) : 0;
$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$content = isset($_POST['content']) ? trim($_POST['content']) : '';

if($post_id > 0 && $user_id > 0 && !empty($content)){
    $query = "INSERT INTO mobile_comments (post_id, user_id, content) VALUES (:post_id, :user_id, :content)";
    $stmt = $conn->prepare($query);

    $stmt->bindParam(':post_id', $post_id);
    $stmt->bindParam(':user_id', $user_id);
    $stmt->bindParam(':content', $content);

    if($stmt->execute()){
        $commentId = (int)$conn->lastInsertId();
        $moderation = new MobileAiModeration($conn);
        $review = $moderation->inspect($content);
        $moderation->recordComment($commentId, $review);
        http_response_code(200);
        echo json_encode(array("success" => true, "message" => "Comment added successfully."));
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Unable to add comment."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Incomplete comment data."));
}
?>