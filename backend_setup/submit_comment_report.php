<?php
// submit_comment_report.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$comment_id = isset($_POST['comment_id']) ? intval($_POST['comment_id']) : 0;
$reporter_user_id = isset($_POST['reporter_user_id']) ? intval($_POST['reporter_user_id']) : 1;
$reason = isset($_POST['reason']) ? trim($_POST['reason']) : 'Inappropriate content';
$additional_comments = isset($_POST['additional_comments']) ? trim($_POST['additional_comments']) : '';

if($comment_id > 0){
    // Auto-create mobile_comment_reports table if it doesn't exist
    $createTableQuery = "CREATE TABLE IF NOT EXISTS `mobile_comment_reports` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `comment_id` int(11) NOT NULL,
      `reporter_user_id` int(11) NOT NULL,
      `reason` varchar(255) DEFAULT NULL,
      `additional_comments` text DEFAULT NULL,
      `status` varchar(50) DEFAULT 'Pending',
      `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    $conn->exec($createTableQuery);

    if ($reporter_user_id <= 0) $reporter_user_id = 1;

    $query = "INSERT INTO mobile_comment_reports (comment_id, reporter_user_id, reason, additional_comments, status) VALUES (:comment_id, :reporter_user_id, :reason, :additional_comments, 'Pending')";
    $stmt = $conn->prepare($query);

    $stmt->bindParam(':comment_id', $comment_id);
    $stmt->bindParam(':reporter_user_id', $reporter_user_id);
    $stmt->bindParam(':reason', $reason);
    $stmt->bindParam(':additional_comments', $additional_comments);

    if($stmt->execute()){
        http_response_code(200);
        echo json_encode(array("success" => true, "message" => "Comment report submitted successfully to moderators."));
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Unable to submit comment report."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Invalid comment ID."));
}
?>