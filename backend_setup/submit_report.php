<?php
// submit_report.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$post_id = isset($_POST['post_id']) ? intval($_POST['post_id']) : 0;
$reporter_user_id = isset($_POST['reporter_user_id']) ? intval($_POST['reporter_user_id']) : 1;
$reason = isset($_POST['reason']) ? trim($_POST['reason']) : 'Inappropriate content';
$additional_comments = isset($_POST['additional_comments']) ? trim($_POST['additional_comments']) : '';

if($post_id > 0){
    // Auto-create mobile_reports table if it doesn't exist
    $createTableQuery = "CREATE TABLE IF NOT EXISTS `mobile_reports` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `post_id` int(11) NOT NULL,
      `reporter_user_id` int(11) NOT NULL,
      `reason` varchar(255) DEFAULT NULL,
      `additional_comments` text DEFAULT NULL,
      `status` varchar(50) DEFAULT 'Pending',
      `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    $conn->exec($createTableQuery);

    if ($reporter_user_id <= 0) $reporter_user_id = 1;

    $query = "INSERT INTO mobile_reports (post_id, reporter_user_id, reason, additional_comments, status) VALUES (:post_id, :reporter_user_id, :reason, :additional_comments, 'Pending')";
    $stmt = $conn->prepare($query);

    $stmt->bindParam(':post_id', $post_id);
    $stmt->bindParam(':reporter_user_id', $reporter_user_id);
    $stmt->bindParam(':reason', $reason);
    $stmt->bindParam(':additional_comments', $additional_comments);

    if($stmt->execute()){
        http_response_code(200);
        echo json_encode(array("success" => true, "message" => "Report submitted successfully to moderators."));
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Unable to submit report."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Invalid post ID."));
}
?>