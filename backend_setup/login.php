<?php
// login.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$email = isset($_POST['email']) ? $_POST['email'] : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

if(!empty($email) && !empty($password)){
    // Fetch user from database
    $query = "SELECT id, institutional_email, full_name, password_hash FROM mobile_users WHERE institutional_email = :email LIMIT 0,1";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(':email', $email);
    $stmt->execute();

    $num = $stmt->rowCount();

    if($num > 0){
        $row = $stmt->fetch(PDO::FETCH_ASSOC);

        // Verify Password
        if(password_verify($password, $row['password_hash'])){
            // Login Success
            $user_arr = array(
                "id" => $row['id'],
                "email" => $row['institutional_email'],
                "full_name" => $row['full_name']
            );

            http_response_code(200);
            echo json_encode(array(
                "success" => true,
                "message" => "Login successful.",
                "user" => $user_arr
            ));
        } else {
            // Wrong Password
            http_response_code(200);
            echo json_encode(array("success" => false, "message" => "Wrong password"));
        }
    } else {
        // User not found
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Wrong email"));
    }
} else {
    // Bad request
    http_response_code(400);
    echo json_encode(array("success" => false, "message" => "Incomplete data."));
}
?>