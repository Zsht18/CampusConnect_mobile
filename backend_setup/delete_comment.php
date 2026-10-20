<?php
// delete_comment.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$comment_id = isset($_POST['comment_id']) ? intval($_POST['comment_id']) : 0;
$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;

if($comment_id > 0 && $user_id > 0){
    $query = "DELETE FROM mobile_comments WHERE id = :comment_id AND user_id = :user_id";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(':comment_id', $comment_id);
    $stmt->bindParam(':user_id', $user_id);

    if($stmt->execute()){
        http_response_code(200);
        echo json_encode(array("success" => true, "message" => "Comment deleted successfully."));
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Unable to delete comment."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Invalid comment or user ID."));
}
?>