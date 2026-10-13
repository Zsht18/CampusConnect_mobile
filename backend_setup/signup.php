<?php
// signup.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$email = isset($_POST['email']) ? $_POST['email'] : '';
$id_number = isset($_POST['id_number']) ? $_POST['id_number'] : '';
$full_name = isset($_POST['full_name']) ? $_POST['full_name'] : '';
$username = isset($_POST['username']) ? $_POST['username'] : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

if(!empty($email) && !empty($id_number) && !empty($full_name) && !empty($username) && !empty($password)){

    // Hash the password for security
    $password_hash = password_hash($password, PASSWORD_BCRYPT);

    $query = "INSERT INTO mobile_users SET institutional_email=:email, id_number=:id_number, full_name=:full_name, username=:username, password_hash=:password_hash";

    $stmt = $conn->prepare($query);

    $stmt->bindParam(":email", $email);
    $stmt->bindParam(":id_number", $id_number);
    $stmt->bindParam(":full_name", $full_name);
    $stmt->bindParam(":username", $username);
    $stmt->bindParam(":password_hash", $password_hash);

    try {
        if($stmt->execute()){
            http_response_code(201);
            echo json_encode(array("success" => true, "message" => "User was created."));
        } else {
            http_response_code(503);
            echo json_encode(array("success" => false, "message" => "Unable to create user."));
        }
    } catch(PDOException $e) {
        // Catch duplicate email/username errors
        http_response_code(400);
        echo json_encode(array("success" => false, "message" => "Error: User with this email, ID, or username already exists."));
    }
} else {
    http_response_code(400);
    echo json_encode(array("success" => false, "message" => "Incomplete data. Please fill all fields."));
}
?>