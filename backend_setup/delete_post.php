<?php
// delete_post.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$post_id = isset($_POST['post_id']) ? intval($_POST['post_id']) : 0;
$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;

if($post_id > 0 && $user_id > 0){
    $query = "DELETE FROM mobile_posts WHERE id = :post_id AND user_id = :user_id";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(':post_id', $post_id);
    $stmt->bindParam(':user_id', $user_id);

    if($stmt->execute()){
        http_response_code(200);
        echo json_encode(array("success" => true, "message" => "Post deleted successfully."));
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Unable to delete post."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Invalid post or user ID."));
}
?>