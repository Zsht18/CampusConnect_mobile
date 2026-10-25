<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$token = isset($_POST['session_token']) ? trim((string)$_POST['session_token']) : '';
if (!preg_match('/^[a-f0-9]{64}$/', $token)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "A valid session token is required."]);
    exit;
}

$statement = $conn->prepare(
    "UPDATE admin_mobile_login_logs
     SET logout_at = COALESCE(logout_at, CURRENT_TIMESTAMP),
         last_activity_at = CASE
             WHEN logout_at IS NULL THEN CURRENT_TIMESTAMP
             ELSE last_activity_at
         END
     WHERE session_token_hash = ?"
);
$statement->execute([hash('sha256', $token)]);
if ($statement->rowCount() === 0) {
    $check = $conn->prepare(
        "SELECT 1 FROM admin_mobile_login_logs
         WHERE session_token_hash = ? AND logout_at IS NOT NULL LIMIT 1"
    );
    $check->execute([hash('sha256', $token)]);
    if ($check->fetchColumn() === false) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Mobile session could not be found."]);
        exit;
    }
}

echo json_encode(["success" => true, "message" => "Logout recorded."]);
