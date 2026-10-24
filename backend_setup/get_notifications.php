<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST");

include_once 'db_connect.php';

$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : intval($_POST['user_id'] ?? 0);
if ($user_id <= 0) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Invalid user ID."]);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['notification_id'])) {
    $mark = $conn->prepare('UPDATE mobile_notifications SET is_read = 1 WHERE id = ? AND user_id = ?');
    $mark->execute([(int)$_POST['notification_id'], $user_id]);
}

$stmt = $conn->prepare(
    'SELECT id, type, title, message, post_id, is_read, created_at
     FROM mobile_notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 100'
);
$stmt->execute([$user_id]);
echo json_encode($stmt->fetchAll(PDO::FETCH_ASSOC));
?>
