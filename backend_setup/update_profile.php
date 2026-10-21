<?php
// update_profile.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$username = isset($_POST['username']) ? trim($_POST['username']) : '';
$old_password = isset($_POST['old_password']) ? trim($_POST['old_password']) : '';
$new_password = isset($_POST['new_password']) ? trim($_POST['new_password']) : '';

if($user_id > 0){
    $updates = array();
    $params = array(':user_id' => $user_id);

    if(!empty($username)){
        // Check unique username (case-insensitive)
        $checkQuery = "SELECT id FROM mobile_users WHERE LOWER(username) = LOWER(:username) AND id != :user_id";
        $checkStmt = $conn->prepare($checkQuery);
        $checkStmt->bindParam(':username', $username);
        $checkStmt->bindParam(':user_id', $user_id);
        $checkStmt->execute();
        if($checkStmt->rowCount() > 0){
            http_response_code(200);
            echo json_encode(array("success" => false, "message" => "Username is already taken. Please choose another."));
            exit();
        }
        $updates[] = "username = :username";
        $params[':username'] = $username;
    }

    if(!empty($new_password)){
        if(empty($old_password)){
            http_response_code(200);
            echo json_encode(array("success" => false, "message" => "Please enter your current password."));
            exit();
        }

        // Verify current password
        $passQuery = "SELECT password_hash FROM mobile_users WHERE id = :user_id";
        $passStmt = $conn->prepare($passQuery);
        $passStmt->bindParam(':user_id', $user_id);
        $passStmt->execute();
        $userRow = $passStmt->fetch(PDO::FETCH_ASSOC);

        if(!$userRow || !password_verify($old_password, $userRow['password_hash'])){
            http_response_code(200);
            echo json_encode(array("success" => false, "message" => "Incorrect current password."));
            exit();
        }

        $password_hash = password_hash($new_password, PASSWORD_BCRYPT);
        $updates[] = "password_hash = :password_hash";
        $params[':password_hash'] = $password_hash;
    }

    if(count($updates) > 0){
        $query = "UPDATE mobile_users SET " . implode(", ", $updates) . " WHERE id = :user_id";
        $stmt = $conn->prepare($query);
        if($stmt->execute($params)){
            http_response_code(200);
            echo json_encode(array("success" => true, "message" => "Profile updated successfully."));
        } else {
            http_response_code(200);
            echo json_encode(array("success" => false, "message" => "Failed to update profile."));
        }
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "No changes provided."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Invalid user ID."));
}
?>