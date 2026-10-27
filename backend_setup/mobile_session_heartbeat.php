<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$token = isset($_POST['session_token']) ? trim((string)$_POST['session_token']) : '';
$userId = isset($_POST['user_id']) ? filter_var($_POST['user_id'], FILTER_VALIDATE_INT) : false;
$session = null;
if ($token !== '' && preg_match('/^[a-f0-9]{64}$/', $token)) {
    $sessionQuery = $conn->prepare(
        "SELECT id, mobile_user_id, logout_at
         FROM admin_mobile_login_logs
         WHERE session_token_hash = ?
         LIMIT 1"
    );
    $sessionQuery->execute([hash('sha256', $token)]);
    $session = $sessionQuery->fetch(PDO::FETCH_ASSOC) ?: null;
} elseif ($token !== '') {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "A valid session token is required."]);
    exit;
}

if ($session === null && (!$userId || $userId <= 0)) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "A valid mobile session is required."]);
    exit;
}

if ($session !== null) {
    $userId = (int)$session['mobile_user_id'];
} else {
    $userExists = $conn->prepare("SELECT 1 FROM mobile_users WHERE id = ? LIMIT 1");
    $userExists->execute([$userId]);
    if (!$userExists->fetchColumn()) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Mobile session is no longer active."]);
        exit;
    }
}
$expireSuspension = $conn->prepare(
    "UPDATE admin_mobile_user_moderation
     SET status = 'active', suspended_until = NULL, suspension_timezone_offset = NULL,
         reason = NULL, moderated_at = CURRENT_TIMESTAMP
     WHERE mobile_user_id = ?
       AND status = 'suspended'
       AND suspended_until IS NOT NULL
       AND (
           (suspension_timezone_offset IS NOT NULL AND suspended_until <= UTC_TIMESTAMP())
           OR (suspension_timezone_offset IS NULL AND suspended_until <= CURRENT_TIMESTAMP)
       )"
);
$expireSuspension->execute([$userId]);

$moderation = $conn->prepare(
    "SELECT status
     FROM admin_mobile_user_moderation
     WHERE mobile_user_id = ?
     LIMIT 1"
);
$moderation->execute([$userId]);
$moderation = $moderation->fetch(PDO::FETCH_ASSOC) ?: [];
$accountStatus = (string)($moderation['status'] ?? 'active');
if (in_array($accountStatus, ['suspended', 'banned'], true)) {
    if ($session !== null) {
        $closeSession = $conn->prepare(
            "UPDATE admin_mobile_login_logs
             SET logout_at = COALESCE(logout_at, CURRENT_TIMESTAMP),
                 last_activity_at = CASE
                     WHEN logout_at IS NULL THEN CURRENT_TIMESTAMP
                     ELSE last_activity_at
                 END
             WHERE id = ?"
        );
        $closeSession->execute([(int)$session['id']]);
    }

    http_response_code(200);
    echo json_encode([
        "success" => false,
        "account_status" => $accountStatus,
        "message" => "Your account has been " . $accountStatus
            . " in violation of the CampusConnect community guidelines.",
    ]);
    exit;
}

if ($session !== null && $session['logout_at'] !== null) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Mobile session is no longer active."]);
    exit;
}

if ($session !== null) {
    $updateActivity = $conn->prepare(
        "UPDATE admin_mobile_login_logs
         SET last_activity_at = CURRENT_TIMESTAMP
         WHERE id = ? AND logout_at IS NULL"
    );
    $updateActivity->execute([(int)$session['id']]);
}

echo json_encode(["success" => true, "message" => "Session activity recorded."]);
